package edu.jhu.srl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarSet;
import edu.jhu.srl.DepParseFactorGraph.DepParseFactorGraphPrm;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;

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
    public static class JointFactorGraphPrm implements Serializable {
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

    public JointNlpFactorGraph(JointFactorGraphPrm prm, SimpleAnnoSentence sent, Set<Integer> knownPreds, CorpusStatistics cs, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc) {
        this(prm, sent.getWords(), sent.getLemmas(), knownPreds, cs.roleStateNames, cs.predSenseListMap, obsFe, ofc);
    }
    
    public JointNlpFactorGraph(JointFactorGraphPrm prm, List<String> words, List<String> lemmas, Set<Integer> knownPreds,
            List<String> roleStateNames, Map<String,List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc) {
        this(prm);
        build(words, lemmas, knownPreds, roleStateNames, psMap, obsFe, ofc, this);
    }
    
    public JointNlpFactorGraph(JointFactorGraphPrm prm) {
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(SimpleAnnoSentence sent, Set<Integer> knownPreds, CorpusStatistics cs, ObsFeatureExtractor obsFe,
            ObsFeatureConjoiner ofc, FactorGraph fg) {
        build(sent.getWords(), sent.getLemmas(), knownPreds, cs.roleStateNames, cs.predSenseListMap, obsFe, ofc, fg);
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(List<String> words, List<String> lemmas, Set<Integer> knownPreds, List<String> roleStateNames,
            Map<String, List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FactorGraph fg) {
        this.n = words.size();

        if (prm.includeDp) {
            dp = new DepParseFactorGraph(prm.dpPrm);
            dp.build(words, obsFe, ofc, fg);
        }
        if (prm.includeSrl) {
            srl = new SrlFactorGraph(prm.srlPrm); 
            srl.build(words, lemmas, knownPreds, roleStateNames, psMap, obsFe, ofc, fg);
        }
        
        // Add the joint factors.
        LinkVar[][] childVars = dp.getChildVars();
        RoleVar[][] roleVars = srl.getRoleVars();
        for (int i = -1; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != -1) {
                    // Add binary factors between Roles and Links.
                    if (roleVars[i][j] != null && childVars[i][j] != null) {
                        addFactor(new TypedFactor(new VarSet(roleVars[i][j], childVars[i][j]), JointFactorTemplate.LINK_ROLE_BINARY, ofc, obsFe));
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
        return dp.getLinkVar(parent, child);
    }

    /**
     * Gets a Role variable.
     * @param i The parent position.
     * @param j The child position.
     * @return The role variable or null if it doesn't exist.
     */
    public RoleVar getRoleVar(int i, int j) {
        return srl.getRoleVar(i, j);
    }
    
    /**
     * Gets a predicate Sense variable.
     * @param i The position of the predicate.
     * @return The sense variable or null if it doesn't exist.
     */
    public SenseVar getSenseVar(int i) {
        return srl.getSenseVar(i);
    }

    public int getSentenceLength() {
        return n;
    }
    
}
