package edu.jhu.nlp.depparse;

import java.io.Serializable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.model.ClampFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
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

    private static final Logger log = LoggerFactory.getLogger(DepParseFactorGraphBuilder.class); 

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
        
        /** Whether to include 2nd-order head-bigram factors in the model. */
        public boolean headBigramFactors = false;
        
        /** Whether to exclude non-projective grandparent factors. */
        public boolean excludeNonprojectiveGrandparents = true;
        
        /** Whether to prune edges not in the pruning mask. */
        public boolean pruneEdges = false;
        
    }
    
    public enum DepParseFactorTemplate {
        LINK_UNARY, LINK_GRANDPARENT, LINK_SIBLING
    }
    
    public static class GraFeTypedFactor extends FeTypedFactor {
        private static final long serialVersionUID = 1L;
        public int p,c,g;
        public GraFeTypedFactor(VarSet vars, Enum<?> type, FeatureExtractor fe, int p, int c, int g) {
            super(vars, type, fe);
            this.p = p;
            this.c = c;
            this.g = g;
        }        
    }
    
    public static class SibFeTypedFactor extends FeTypedFactor {
        private static final long serialVersionUID = 1L;
        public int p,c,s;
        public SibFeTypedFactor(VarSet vars, Enum<?> type, FeatureExtractor fe, int p, int c, int s) {
            super(vars, type, fe);
            this.p = p;
            this.c = c;
            this.s = s;
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
        
        if (!prm.pruneEdges || depEdgeMask == null) {
            // Keep all edges
            depEdgeMask = new DepEdgeMask(words.size(), true);
        }
        
        // Don't include factors on observed variables.
        if (prm.linkVarType == VarType.OBSERVED) {
            return;
        }
        
        addClampFactors(fg, depEdgeMask, fe);
        if (prm.unaryFactors) { addUnaryFactors(fg, depEdgeMask, fe); }
        if (prm.grandparentFactors) { addGrandparentFactors(fg, depEdgeMask, fe); }
        if (prm.siblingFactors) { addArbitrarySiblingFactors(fg, depEdgeMask, fe); }
    }

    private void addClampFactors(FactorGraph fg, DepEdgeMask depEdgeMask, FeatureExtractor fe) {
        // Add factors clamping the pruned edges to be "off".
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                LinkVar pcVar = getLinkVar(p, c);
                if (pcVar == null) { continue; }
                if (depEdgeMask.isPruned(p, c)) {
                    // This edge will never be "on".
                    fg.addFactor(new ClampFactor(pcVar, LinkVar.FALSE));
                }
            }
        }
    }
    
    private void addUnaryFactors(FactorGraph fg, DepEdgeMask depEdgeMask, FeatureExtractor fe) {
        // Add unary factors on root / child Links.
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                LinkVar pcVar = getLinkVar(p, c);
                if (pcVar == null) { continue; }
                if (depEdgeMask.isPruned(p, c)) { continue; }
                fg.addFactor(new FeTypedFactor(new VarSet(pcVar), DepParseFactorTemplate.LINK_UNARY, fe));
            }
        }
    }
    
    private void addGrandparentFactors(FactorGraph fg, DepEdgeMask depEdgeMask, FeatureExtractor fe) {
        // Add grandparent factors.
        for (int g = -1; g < n; g++) {
            for (int p = 0; p < n; p++) {
                if (g == p) { continue; }
                LinkVar gpVar = getLinkVar(g, p);
                if (depEdgeMask.isPruned(g, p) || gpVar == null) { continue; }
                for (int c = 0; c < n; c++) {
                    if (g == c || p == c) { continue; }                  
                    LinkVar pcVar = getLinkVar(p, c);
                    if (pcVar == null || depEdgeMask.isPruned(p, c)) { continue; }
                    boolean isNonprojectiveGrandparent = (g < p && c < g) || (p < g && g < c);
                    if (prm.excludeNonprojectiveGrandparents && isNonprojectiveGrandparent) { continue; }  
                    fg.addFactor(new GraFeTypedFactor(new VarSet(gpVar, pcVar), DepParseFactorTemplate.LINK_GRANDPARENT, fe, p, c, g));
                }
            }
        }
    }
    
    private void addArbitrarySiblingFactors(FactorGraph fg, DepEdgeMask depEdgeMask, FeatureExtractor fe) {
        // Add arbitrary sibling factors.
        for (int p = -1; p < n; p++) {
            for (int c = 0; c < n; c++) {
                if (p == c) { continue; }
                LinkVar pcVar = getLinkVar(p, c);
                if (depEdgeMask.isPruned(p, c) || pcVar == null) { continue; }
                for (int s = 0; s < n; s++) {
                    if (p == s || c == s) { continue; }
                    LinkVar ikVar = getLinkVar(p, s);
                    if (ikVar == null || depEdgeMask.isPruned(p, s)) { continue; }
                    if (c < s) {
                        fg.addFactor(new SibFeTypedFactor(new VarSet(pcVar, ikVar), DepParseFactorTemplate.LINK_SIBLING, fe, p, c, s));                        
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
