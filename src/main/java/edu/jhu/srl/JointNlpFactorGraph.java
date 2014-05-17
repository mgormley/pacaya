package edu.jhu.srl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.set.IntSet;
import edu.jhu.srl.DepParseFactorGraph.DepParseFactorGraphPrm;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.util.Prm;

/**
 * A factor graph builder for joint dependency parsing and semantic role
 * labeling. Note this class also extends FactorGraph in order to provide easy
 * lookups of cached variables.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class JointNlpFactorGraph extends FactorGraph {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(JointNlpFactorGraph.class); 

    /**
     * Parameters for the SrlFactorGraph.
     * @author mgormley
     */
    public static class JointFactorGraphPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public boolean includeDp = true;
        public DepParseFactorGraphPrm dpPrm = new DepParseFactorGraphPrm();
        public boolean includeSrl = true;
        public SrlFactorGraphPrm srlPrm = new SrlFactorGraphPrm();
    }
    
    public enum JointFactorTemplate {
        LINK_ROLE_BINARY,
    }
    
    // Parameters for constructing the factor graph.
    private JointFactorGraphPrm prm;

    // The sentence length.
    private int n;
    
    // Factor graph builders, which also cache the variables.
    private DepParseFactorGraph dp;  
    private SrlFactorGraph srl;

    public JointNlpFactorGraph(JointFactorGraphPrm prm, AnnoSentence sent, CorpusStatistics cs, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FeatureExtractor fe) {
        this(prm, sent.getWords(), sent.getLemmas(), sent.getDepEdgeMask(), sent.getKnownPreds(), cs.roleStateNames, cs.predSenseListMap, obsFe, ofc, fe);
    }
    
    public JointNlpFactorGraph(JointFactorGraphPrm prm, List<String> words, List<String> lemmas, DepEdgeMask depEdgeMask,
            IntSet knownPreds, List<String> roleStateNames, Map<String,List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FeatureExtractor fe) {
        this(prm);
        build(words, lemmas, knownPreds, roleStateNames, psMap, obsFe, ofc, fe, this, depEdgeMask);
    }
    
    public JointNlpFactorGraph(JointFactorGraphPrm prm) {
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     * @param fe TODO
     */
    public void build(AnnoSentence sent, CorpusStatistics cs, ObsFeatureExtractor obsFe,
            ObsFeatureConjoiner ofc, FeatureExtractor fe, FactorGraph fg) {
        build(sent.getWords(), sent.getLemmas(), sent.getKnownPreds(), cs.roleStateNames, cs.predSenseListMap, obsFe, ofc, fe, fg, sent.getDepEdgeMask());
    }

    /**
     * Adds factors and variables to the given factor graph.
     * @param fe TODO
     * @param depEdgeMask TODO
     */
    public void build(List<String> words, List<String> lemmas, IntSet knownPreds, List<String> roleStateNames,
            Map<String, List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FeatureExtractor fe, FactorGraph fg, DepEdgeMask depEdgeMask) {
        this.n = words.size();

        if (prm.includeDp) {
            dp = new DepParseFactorGraph(prm.dpPrm);
            dp.build(words, depEdgeMask, fe, fg);
        }
        if (prm.includeSrl) {
            srl = new SrlFactorGraph(prm.srlPrm); 
            srl.build(words, lemmas, knownPreds, roleStateNames, psMap, obsFe, ofc, fg);
        }
        
        if (prm.includeDp && prm.includeSrl) {
            // Add the joint factors.
            LinkVar[][] childVars = dp.getChildVars();
            RoleVar[][] roleVars = srl.getRoleVars();
            for (int i = -1; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i != -1) {
                        // Add binary factors between Roles and Links.
                        if (roleVars[i][j] != null && childVars[i][j] != null) {
                            addFactor(new ObsFeTypedFactor(new VarSet(roleVars[i][j], childVars[i][j]), JointFactorTemplate.LINK_ROLE_BINARY, ofc, obsFe));
                        }
                    }
                }
            }
        }
    }

    // ----------------- Creating Variables -----------------

    // ----------------- Public Getters -----------------
    
    /**
     * Get the link var corresponding to the specified parent and child position.
     * 
     * @param parent The parent word position, or -1 to indicate the wall node.
     * @param child The child word position.
     * @return The link variable or null if it doesn't exist.
     */
    public LinkVar getLinkVar(int parent, int child) {
        if (dp == null) { return null; }
        return dp.getLinkVar(parent, child);
    }

    /**
     * Gets a Role variable.
     * @param i The parent position.
     * @param j The child position.
     * @return The role variable or null if it doesn't exist.
     */
    public RoleVar getRoleVar(int i, int j) {
        if (srl == null) { return null; }
        return srl.getRoleVar(i, j);
    }
    
    /**
     * Gets a predicate Sense variable.
     * @param i The position of the predicate.
     * @return The sense variable or null if it doesn't exist.
     */
    public SenseVar getSenseVar(int i) {
        if (srl == null) { return null; }
        return srl.getSenseVar(i);
    }

    public int getSentenceLength() {
        return n;
    }
    
}
