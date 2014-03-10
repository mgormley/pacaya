package edu.jhu.srl;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.ProjDepTreeFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;

/**
 * A factor graph builder for syntactic dependency parsing.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class DepParseFactorGraph implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DepParseFactorGraph.class); 

    /**
     * Parameters for the DepParseFactorGraph.
     * @author mgormley
     */
    public static class DepParseFactorGraphPrm implements Serializable {

        private static final long serialVersionUID = 1L;

        /** The type of the link variables. */
        public VarType linkVarType = VarType.LATENT;
        
        /**
         * Whether to include a global factor which constrains the Link
         * variables to form a projective dependency tree.
         */
        public boolean useProjDepTreeFactor = false;
        
        /** Whether to include unary factors in the model. (Ignored if there are no Link variables.) */
        public boolean unaryFactors = true;
    }
    
    public enum DepParseFactorTemplate {
        LINK_UNARY,
    }
    
    // Parameters for constructing the factor graph.
    private DepParseFactorGraphPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    // The sentence length.
    private int n;

    public DepParseFactorGraph(DepParseFactorGraphPrm prm) {        
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(SimpleAnnoSentence sent, Set<Integer> knownPreds, CorpusStatistics cs, ObsFeatureExtractor obsFe,
            ObsFeatureConjoiner ofc, FactorGraph fg) {
        build(sent.getWords(), obsFe, ofc, fg);
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(List<String> words, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FactorGraph fg) {
        this.n = words.size();
        
        // Create the Link variables.
        if (prm.useProjDepTreeFactor && prm.linkVarType != VarType.OBSERVED) {
            log.trace("Adding projective dependency tree global factor.");
            ProjDepTreeFactor treeFactor = new ProjDepTreeFactor(n, prm.linkVarType);
            rootVars = treeFactor.getRootVars();
            childVars = treeFactor.getChildVars();
            // Add the global factor.
            fg.addFactor(treeFactor);
        } else {
            log.trace("Adding Link variables, without the global factor.");
            rootVars = new LinkVar[n];
            childVars = new LinkVar[n][n];
            for (int i = -1; i < n; i++) {
                for (int j = 0; j < n;j++) {
                    if (i != j) {
                        if (i == -1) {
                            rootVars[j] = createLinkVar(i, j);
                        } else {
                            childVars[i][j] = createLinkVar(i, j);
                        }
                    }
                }
            }
        }
        
        // Add the factors.
        for (int i = -1; i < n; i++) {
            // Add the role/link factors.
            for (int j = 0; j < n; j++) {
                if (i == -1) {
                    // Add unary factors on root Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && rootVars[j] != null) {
                        fg.addFactor(new ObsFeTypedFactor(new VarSet(rootVars[j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe));
                    }
                } else {
                    // Add unary factors on child Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && childVars[i][j] != null) {
                        fg.addFactor(new ObsFeTypedFactor(new VarSet(childVars[i][j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe));
                    }
                }
            }
        }
    }

    // ----------------- Creating Variables -----------------

    private LinkVar createLinkVar(int parent, int child) {
        String linkVarName = LinkVar.getDefaultName(parent,  child);
        return new LinkVar(prm.linkVarType, linkVarName, parent, child);
    }
    
    // ----------------- Public Getters -----------------
    
    /**
     * Get the link var corresponding to the specified parent and child position.
     * 
     * @param parent The parent word position, or -1 to indicate the wall node.
     * @param child The child word position.
     * @return The link variable or null if it doesn't exist.
     */
    public LinkVar getLinkVar(int parent, int child) {
        if (! (-1 <= parent && parent < n && 0 <= child && child < n)) {
            return null;
        }
        
        if (parent == -1) {
            return rootVars[child];
        } else {
            return childVars[parent][child];
        }
    }

    public int getSentenceLength() {
        return n;
    }

    public LinkVar[][] getChildVars() {
        return childVars;
    }
    
}
