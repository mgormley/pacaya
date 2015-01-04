package edu.jhu.nlp.srl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.ObsFeTypedFactor;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.prim.iter.IntIter;
import edu.jhu.prim.set.IntSet;
import edu.jhu.util.collections.Lists;

/**
 * A factor graph builder for SRL.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SrlFactorGraphBuilder implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TEMPLATE_KEY_FOR_UNKNOWN_SENSE = SrlFactorTemplate.SENSE_UNARY + "_" + CorpusStatistics.UNKNOWN_SENSE;
    public static final String TEMPLATE_KEY_FOR_UNKNOWN_SENSE_ROLE = SrlFactorTemplate.SENSE_ROLE_BINARY + "_" + CorpusStatistics.UNKNOWN_SENSE;
    private static final Logger log = LoggerFactory.getLogger(SrlFactorGraphBuilder.class); 

    /**
     * Parameters for the SrlFactorGraph.
     * @author mgormley
     */
    public static class SrlFactorGraphBuilderPrm implements Serializable {

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
        
        /** Whether to include factors between the sense and role variables. */
        public boolean binarySenseRoleFactors = false;
        
        /** Whether to predict the predicate sense. */
        public boolean predictSense = false;
        
        /** Whether to predict the predicate positions. */
        public boolean predictPredPos = false;
    }

    public enum RoleStructure {
        /** Defines Role variables each of the "known" predicates with all possible arguments. */
        PREDS_GIVEN,
        /** The N**2 model. */
        ALL_PAIRS,
        /** Do not predict roles. */
        NO_ROLES,
    }
    
    public enum SrlFactorTemplate {
        ROLE_UNARY,
        SENSE_UNARY, 
        SENSE_ROLE_BINARY,
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
    private SrlFactorGraphBuilderPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private RoleVar[][] roleVars;
    private SenseVar[] senseVars;

    // The sentence length.
    private int n;                

    public SrlFactorGraphBuilder(SrlFactorGraphBuilderPrm prm) {
        this.prm = prm;
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(AnnoSentence sent, CorpusStatistics cs, ObsFeatureExtractor obsFe,
            ObsFeatureConjoiner ofc, FactorGraph fg) {
        build(sent.getWords(), sent.getLemmas(), sent.getKnownPreds(), cs.roleStateNames, cs.predSenseListMap, obsFe, ofc, fg);
    }

    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(List<String> words, List<String> lemmas, IntSet knownPreds, List<String> roleStateNames,
            Map<String, List<String>> psMap, ObsFeatureExtractor obsFe, ObsFeatureConjoiner ofc, FactorGraph fg) {
        // Check for null arguments.
        if (prm.roleStructure == RoleStructure.PREDS_GIVEN && knownPreds == null) {
            throw new IllegalArgumentException("knownPreds must be non-null");
        }
        if (prm.predictSense && lemmas == null) {
            throw new IllegalArgumentException("lemmas must be non-null");
        }
        if (prm.predictSense && psMap == null) {
            throw new IllegalArgumentException("psMap must be non-null");
        }
        if (prm.roleStructure == RoleStructure.PREDS_GIVEN && prm.predictPredPos) {
            throw new IllegalStateException("PREDS_GIVEN assumes that the predicate positions are always observed.");
        }
        
        this.n = words.size();

        // Create the Role variables.
        roleVars = new RoleVar[n][n];
        if (prm.roleStructure == RoleStructure.PREDS_GIVEN) {
            // CoNLL-friendly model; preds given
            IntIter iter = knownPreds.iterator();
            while (iter.hasNext()) {
                int i = iter.next();
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
        } else if (prm.roleStructure == RoleStructure.NO_ROLES) {
            // No role variables.
        } else {
            throw new IllegalArgumentException("Unsupported model structure: " + prm.roleStructure);
        }
        
        // Create the Sense variables.
        senseVars = new SenseVar[n];
        for (int i = 0; i < n; i++) {
            // Only look at the knownPreds if the predicate positions are given.
            if (prm.roleStructure == RoleStructure.PREDS_GIVEN && !knownPreds.contains(i)) {
                // Skip non-predicate positions.
                continue;
            }
            if ((!prm.predictSense && !prm.predictPredPos) ||  
                    (prm.roleStructure == RoleStructure.PREDS_GIVEN && !prm.predictSense)) {
                // Do not add sense variables.
            } else if (prm.predictSense && prm.predictPredPos) {
                // Sense and position.
                List<String> senseStateNames = psMap.get(lemmas.get(i));
                if (senseStateNames == null) {
                    senseStateNames = CorpusStatistics.PRED_POSITION_STATE_NAMES;
                } else {
                    // Include the state of "no predicate".
                    senseStateNames = Lists.cons("_", senseStateNames);
                }
                senseVars[i] = createSenseVar(i, senseStateNames);
            } else if (prm.predictSense && !prm.predictPredPos) {
                // Sense without positions.
                List<String> senseStateNames = psMap.get(lemmas.get(i));
                if (senseStateNames == null) {
                    senseStateNames = CorpusStatistics.SENSES_FOR_UNK_PRED;
                }
                senseVars[i] = createSenseVar(i, senseStateNames);
            } else if (!prm.predictSense && prm.predictPredPos) {
                // Positions without sense.
                senseVars[i] = createSenseVar(i, CorpusStatistics.PRED_POSITION_STATE_NAMES);
            }
        }

                
        // Add the factors.
        for (int i = -1; i < n; i++) {
            // Get the lemma or UNK if we don't know it.
            String lemmaForTk = null;
            if (i >= 0) {
                if (prm.predictSense && psMap.get(lemmas.get(i)) != null) {
                    // The template key must include the lemma appended, so that
                    // there is a unique set of model parameters for each predicate.
                    lemmaForTk = lemmas.get(i);
                } else {
                    // If we've never seen this predicate, just give it to the (untrained) unknown classifier.
                    lemmaForTk = CorpusStatistics.UNKNOWN_SENSE;
                }
            }
            // Add the unary factors for the sense variables.
            if (i >= 0 && senseVars[i] != null && senseVars[i].getType() != VarType.OBSERVED) {
                String templateKey = SrlFactorTemplate.SENSE_UNARY + "_" + lemmaForTk;
                fg.addFactor(new ObsFeTypedFactor(new VarSet(senseVars[i]), SrlFactorTemplate.SENSE_UNARY, templateKey, ofc, obsFe));
            }
            // Add the role factors.
            for (int j = 0; j < n; j++) {
                if (i != -1) {
                    // Add unary factors on Roles.
                    if (prm.unaryFactors && roleVars[i][j] != null) {
                        fg.addFactor(new ObsFeTypedFactor(new VarSet(roleVars[i][j]), SrlFactorTemplate.ROLE_UNARY, ofc, obsFe));
                    }
                    // Add binary factors between Role and Sense variables.
                    if (prm.binarySenseRoleFactors && senseVars[i] != null && roleVars[i][j] != null) {
                        String templateKey = SrlFactorTemplate.SENSE_ROLE_BINARY + "_" + lemmaForTk;
                        fg.addFactor(new ObsFeTypedFactor(new VarSet(senseVars[i], roleVars[i][j]), SrlFactorTemplate.SENSE_ROLE_BINARY, templateKey, ofc, obsFe));
                    }
                }
            }
        }
    }

    // ----------------- Creating Variables -----------------

    private RoleVar createRoleVar(int parent, int child, IntSet knownPreds, List<String> roleStateNames) {
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
