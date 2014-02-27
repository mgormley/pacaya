package edu.jhu.srl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.ProjDepTreeFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;

/**
 * A factor graph for syntactic dependency parsing.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class DepParseFactorGraph extends FactorGraph {

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
        
        /** Whether to always include Link variables. For testing only. */
        public boolean alwaysIncludeLinkVars = false;
    }
    
    public enum DepParseFactorTemplate {
        LINK_ROLE_BINARY,
        LINK_UNARY,
    }
    
    /**
     * A dependency parsing factor, which includes its type (i.e. template).
     * @author mgormley
     */
    public static class DepParseFactor extends ObsFeExpFamFactor {

        private static final long serialVersionUID = 1L;

        DepParseFactorTemplate type;
        
        public DepParseFactor(VarSet vars, DepParseFactorTemplate type, ObsFeatureConjoiner cj, ObsFeatureExtractor obsFe) {
            super(vars, type, cj, obsFe);
            this.type = type;
        }
        
        /**
         * Constructs an SrlFactor.
         * 
         * This constructor allows us to differentiate between the "type" of
         * factor (e.g. SENSE_UNARY) and its "templateKey" (e.g.
         * SENSE_UNARY_satisfacer.a1). Using Sense factors as an example, this
         * way we can use the type to determine which type of features should be
         * extracted, and the templateKey to determine which independent
         * classifier should be used.
         * 
         * @param vars The variables.
         * @param type The type.
         * @param templateKey The template key.
         */
        public DepParseFactor(VarSet vars, DepParseFactorTemplate type, Object templateKey, ObsFeatureConjoiner cj, ObsFeatureExtractor obsFe) {
            super(vars, templateKey, cj, obsFe);
            this.type = type;
        }
        
        public DepParseFactorTemplate getFactorType() {
            return type;
        }
        
    }
    
    // Parameters for constructing the factor graph.
    private DepParseFactorGraphPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    // The sentence length.
    private final int n;                

    public DepParseFactorGraph(DepParseFactorGraphPrm prm, SimpleAnnoSentence sent, Set<Integer> knownPreds, CorpusStatistics cs, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc) {
        this(prm, sent.getWords(), sent.getLemmas(), knownPreds, cs.roleStateNames, cs.predSenseListMap, obsFe, ofc);
    }

    public DepParseFactorGraph(DepParseFactorGraphPrm prm, List<String> words, List<String> lemmas, Set<Integer> knownPreds,
            List<String> roleStateNames, Map<String,List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc) {
        this.prm = prm;
        this.n = words.size();
        
        // Create the Link variables.
        if (prm.useProjDepTreeFactor && prm.linkVarType != VarType.OBSERVED) {
            log.trace("Adding projective dependency tree global factor.");
            ProjDepTreeFactor treeFactor = new ProjDepTreeFactor(n, prm.linkVarType);
            rootVars = treeFactor.getRootVars();
            childVars = treeFactor.getChildVars();
            // Add the global factor.
            this.addFactor(treeFactor);
        } else if (prm.linkVarType == VarType.OBSERVED || prm.alwaysIncludeLinkVars) {
            log.trace("Adding observed Link variables, without the global factor.");
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
        } else {
            rootVars = new LinkVar[n];
            childVars = new LinkVar[n][n];
            log.trace("Not adding any Link variables.");
            // IMPORTANT NOTE: Here we OVERRIDE prm.unaryFactors to make sure
            // that the Role variables have /some/ factor to participate in.
            prm.unaryFactors = true;
        }
        
        // Add the factors.
        for (int i = -1; i < n; i++) {
            // Add the role/link factors.
            for (int j = 0; j < n; j++) {
                if (i == -1) {
                    // Add unary factors on root Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && rootVars[j] != null) {
                        this.addFactor(new DepParseFactor(new VarSet(rootVars[j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe));
                    }
                } else {
                    // Add unary factors on child Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && childVars[i][j] != null) {
                        this.addFactor(new DepParseFactor(new VarSet(childVars[i][j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe));
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
    
}
