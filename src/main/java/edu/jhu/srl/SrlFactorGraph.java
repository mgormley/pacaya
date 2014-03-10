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
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.set.IntHashSet;

/**
 * A factor graph builder for SRL.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SrlFactorGraph implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TEMPLATE_KEY_FOR_UNKNOWN_SENSE = SrlFactorTemplate.SENSE_UNARY + "_" + CorpusStatistics.UNKNOWN_SENSE;
    private static final Logger log = Logger.getLogger(SrlFactorGraph.class); 

    /**
     * Parameters for the SrlFactorGraph.
     * @author mgormley
     */
    public static class SrlFactorGraphPrm implements Serializable {

        private static final long serialVersionUID = 1L;

        /** The structure of the Role variables. */
        public RoleStructure roleStructure = RoleStructure.ALL_PAIRS;
        
        /**
         * Whether the Role variables (if any) that correspond to predicates not
         * marked with a "Y" should be latent, as opposed to predicted
         * variables.
         */
        public boolean makeUnknownPredRolesLatent = true;
        
        /** Whether to allow a predicate to assign a role to itself. (This should be turned on for English) */
        public boolean allowPredArgSelfLoops = false;
        
        /** Whether to include unary factors in the model. (Ignored if there are no Link variables.) */
        public boolean unaryFactors = true;
        
        /** Whether to predict the predicate sense. */
        public boolean predictSense = false;
    }

    public enum RoleStructure {
        /** Defines Role variables each of the "known" predicates with all possible arguments. */
        PREDS_GIVEN,
        /** The N**2 model. */
        ALL_PAIRS,
    }
    
    public enum SrlFactorTemplate {
        ROLE_UNARY,
        SENSE_UNARY,
    }
    
    /**
     * Role variable.
     * 
     * @author mgormley
     */
    public static class RoleVar extends Var {
        
        private static final long serialVersionUID = 1L;

        private int parent;
        private int child;
        
        public RoleVar(VarType type, int numStates, String name, List<String> stateNames, int parent, int child) {
            super(type, numStates, name, stateNames);
            this.parent = parent;
            this.child = child;
        }

        public int getParent() {
            return parent;
        }

        public int getChild() {
            return child;
        }
        
    }
    
    /**
     * Sense variable. 
     * 
     * @author mgormley
     */
    public static class SenseVar extends Var {

        private static final long serialVersionUID = 1L;

        private int parent;
        
        public SenseVar(VarType type, int numStates, String name, List<String> stateNames, int parent) {
            super(type, numStates, name, stateNames);
            this.parent = parent;
        }

        public int getParent() {
            return parent;
        }

    }

    // Parameters for constructing the factor graph.
    private SrlFactorGraphPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private RoleVar[][] roleVars;
    private SenseVar[] senseVars;

    // The sentence length.
    private int n;                

    public SrlFactorGraph(SrlFactorGraphPrm prm) {
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(SimpleAnnoSentence sent, CorpusStatistics cs, ObsFeatureExtractor obsFe,
            ObsFeatureConjoiner ofc, FactorGraph fg) {
        build(sent.getWords(), sent.getLemmas(), sent.getKnownPreds(), cs.roleStateNames, cs.predSenseListMap, obsFe, ofc, fg);
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(List<String> words, List<String> lemmas, Set<Integer> knownPreds, List<String> roleStateNames,
            Map<String, List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FactorGraph fg) {
        this.n = words.size();

        // Create the Role variables.
        roleVars = new RoleVar[n][n];
        if (prm.roleStructure == RoleStructure.PREDS_GIVEN) {
            // CoNLL-friendly model; preds given
            for (int i : knownPreds) {
                for (int j = 0; j < n;j++) {
                    if (i==j && !prm.allowPredArgSelfLoops) {
                        continue;
                    }
                    roleVars[i][j] = createRoleVar(i, j, knownPreds, roleStateNames);
                }
            }
        } else if (prm.roleStructure == RoleStructure.ALL_PAIRS) {
            // n**2 model
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n;j++) {
                    if (i==j && !prm.allowPredArgSelfLoops) {
                        continue;
                    }
                    roleVars[i][j] = createRoleVar(i, j, knownPreds, roleStateNames);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported model structure: " + prm.roleStructure);
        }
        
        // Create the Sense variables.
        senseVars = new SenseVar[n];
        if (prm.predictSense) {
            for (int i = 0; i < n; i++) {
                if (knownPreds.contains(i)) {
                    List<String> senseStateNames = psMap.get(lemmas.get(i));
                    if (senseStateNames == null) {
                        senseStateNames = CorpusStatistics.SENSES_FOR_UNK_PRED;
                    }
                    senseVars[i] = createSenseVar(i, senseStateNames);
                }
            }
        }
                
        // Add the factors.
        for (int i = -1; i < n; i++) {
            // Add the unary factors for the sense variables.
            if (prm.predictSense && i >= 0 && senseVars[i] != null && senseVars[i].getType() != VarType.OBSERVED) {
                // The template key must include the lemma appended, so that
                // there is a unique set of model parameters for each predicate.
                String templateKey = SrlFactorTemplate.SENSE_UNARY + "_" + lemmas.get(i);
                // If we've never seen this predicate, just give it to the (untrained) unknown classifier.
                if (psMap.get(lemmas.get(i)) == null) {
                    templateKey = TEMPLATE_KEY_FOR_UNKNOWN_SENSE;
                }
                fg.addFactor(new ObsFeTypedFactor(new VarSet(senseVars[i]), SrlFactorTemplate.SENSE_UNARY, templateKey, ofc, obsFe));
            }
            // Add the role/link factors.
            for (int j = 0; j < n; j++) {
                if (i != -1) {
                    // Add unary factors on Roles.
                    if (prm.unaryFactors && roleVars[i][j] != null) {
                        fg.addFactor(new ObsFeTypedFactor(new VarSet(roleVars[i][j]), SrlFactorTemplate.ROLE_UNARY, ofc, obsFe));
                    }
                }
            }
        }
    }

    // ----------------- Creating Variables -----------------

    private RoleVar createRoleVar(int parent, int child, Set<Integer> knownPreds, List<String> roleStateNames) {
        RoleVar roleVar;
        String roleVarName = "Role_" + parent + "_" + child;
        if (!prm.makeUnknownPredRolesLatent || knownPreds.contains((Integer) parent)) {
            roleVar = new RoleVar(VarType.PREDICTED, roleStateNames.size(), roleVarName, roleStateNames, parent, child);            
        } else {
            roleVar = new RoleVar(VarType.LATENT, roleStateNames.size(), roleVarName, roleStateNames, parent, child);
        }
        return roleVar;
    }
    
    private SenseVar createSenseVar(int parent, List<String> senseStateNames) {
        String senseVarName = "Sense_" + parent;
        return new SenseVar(VarType.PREDICTED, senseStateNames.size(), senseVarName, senseStateNames, parent);            
    }
    
    // ----------------- Public Getters -----------------
    
    /**
     * Gets a Role variable.
     * @param i The parent position.
     * @param j The child position.
     * @return The role variable or null if it doesn't exist.
     */
    public RoleVar getRoleVar(int i, int j) {
        if (0 <= i && i < roleVars.length && 0 <= j && j < roleVars[i].length) {
            return roleVars[i][j];
        } else {
            return null;
        }
    }
    
    /**
     * Gets a predicate Sense variable.
     * @param i The position of the predicate.
     * @return The sense variable or null if it doesn't exist.
     */
    public SenseVar getSenseVar(int i) {
        if (0 <= i && i < senseVars.length) {
            return senseVars[i];
        } else {
            return null;
        }
    }

    public int getSentenceLength() {
        return n;
    }

    public RoleVar[][] getRoleVars() {
        return roleVars;
    }
    
}
