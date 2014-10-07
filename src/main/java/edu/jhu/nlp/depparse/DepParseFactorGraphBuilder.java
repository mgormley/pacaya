package edu.jhu.nlp.depparse;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.model.ClampFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.nlp.FeTypedFactor;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;

/**
 * A factor graph builder for syntactic dependency parsing.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class DepParseFactorGraphBuilder implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(DepParseFactorGraphBuilder.class); 

    /**
     * Parameters for the DepParseFactorGraph.
     * @author mgormley
     */
    public static class DepParseFactorGraphBuilderPrm implements Serializable {

        private static final long serialVersionUID = 1L;

        /** The type of the link variables. */
        public VarType linkVarType = VarType.LATENT;
        
        /**
         * Whether to include a global factor which constrains the Link
         * variables to form a projective dependency tree.
         */
        public boolean useProjDepTreeFactor = false;

        /** Whether to include 1st-order unary factors in the model.*/
        public boolean unaryFactors = true;

        /** Whether to include 2nd-order grandparent factors in the model. */
        public boolean grandparentFactors = false;

        /** Whether to include 2nd-order sibling factors in the model. */
        public boolean siblingFactors = false;
        
        /** Whether to exclude non-projective grandparent factors. */
        public boolean excludeNonprojectiveGrandparents = true;
        
        /** Whether to prune edges not in the pruning mask. */
        public boolean pruneEdges = false;
        
    }
    
    public enum DepParseFactorTemplate {
        LINK_UNARY, LINK_GRANDPARENT, LINK_SIBLING
    }
    
    public static class O2FeTypedFactor extends FeTypedFactor {
        private static final long serialVersionUID = 1L;
        public int i,j,k;
        public O2FeTypedFactor(VarSet vars, Enum<?> type, FeatureExtractor fe, int i, int j, int k) {
            super(vars, type, fe);
            this.i = i;
            this.j = j;
            this.k = k;
        }        
    }
    
    // Parameters for constructing the factor graph.
    private DepParseFactorGraphBuilderPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    // The sentence length.
    private int n;

    public DepParseFactorGraphBuilder(DepParseFactorGraphBuilderPrm prm) {        
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(AnnoSentence sent, FeatureExtractor fe, FactorGraph fg) {
        build(sent.getWords(), sent.getDepEdgeMask(), fe, fg);
    }
    
    /**
     * Adds factors and variables to the given factor graph.
     * @param depEdgeMask TODO
     */
    public void build(List<String> words, DepEdgeMask depEdgeMask, FeatureExtractor fe, FactorGraph fg) {
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
        
        if (prm.pruneEdges &&  depEdgeMask == null) {
            throw new IllegalStateException("Cannot prune when dependency edge pruning mask is null");
        }
        if (!prm.pruneEdges) {
            // Keep all edges
            depEdgeMask = new DepEdgeMask(words.size(), true);
        }
        
        // Don't include factors on observed variables.
        if (prm.linkVarType != VarType.OBSERVED) { 
            // Add the factors.
            for (int i = -1; i < n; i++) {
                // Add the role/link factors.
                for (int j = 0; j < n; j++) {
                    if (i == j) { continue; }
                    LinkVar ijVar = getLinkVar(i, j);
                    if (ijVar != null) {
                        if (depEdgeMask.isPruned(i, j)) {
                            // This edge will never be "on".
                            fg.addFactor(new ClampFactor(ijVar, LinkVar.FALSE));
                        } else {
                            // Add unary factors on root / child Links
                            if (prm.unaryFactors) {
                                fg.addFactor(new FeTypedFactor(new VarSet(ijVar), DepParseFactorTemplate.LINK_UNARY, fe));
                            }
                            for (int k = 0; k < n; k++) {
                                if (i == k || j == k) { continue; }
                                // Add grandparent factors.
                                boolean isNonprojectiveGrandparent = (i < j && k < i) || (j < i && i < k);
                                if (!prm.excludeNonprojectiveGrandparents || !isNonprojectiveGrandparent) {
                                    LinkVar jkVar = getLinkVar(j, k);
                                    if (prm.grandparentFactors && jkVar != null && !depEdgeMask.isPruned(j, k)) {
                                        fg.addFactor(new O2FeTypedFactor(new VarSet(ijVar, jkVar), DepParseFactorTemplate.LINK_GRANDPARENT, fe, i, j, k));
                                    }
                                }
                                if (j < k) {
                                    // Add sibling factors.
                                    LinkVar ikVar = getLinkVar(i, k);
                                    if (prm.siblingFactors && ikVar != null && !depEdgeMask.isPruned(i, k)) {
                                        fg.addFactor(new O2FeTypedFactor(new VarSet(ijVar, ikVar), DepParseFactorTemplate.LINK_SIBLING, fe, i, j, k));
                                    }
                                }
                            }
                        }
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
