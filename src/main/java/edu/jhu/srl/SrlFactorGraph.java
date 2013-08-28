package edu.jhu.srl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.ExpFamFactor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.ProjDepTreeFactor;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarSet;

/**
 * A factor graph for SRL.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SrlFactorGraph extends FactorGraph {

    public static final String TEMPLATE_KEY_FOR_UNKNOWN_SENSE = SrlFactorTemplate.SENSE_UNARY + "_" + CorpusStatistics.UNKNOWN_SENSE;

    private static final Logger log = Logger.getLogger(SrlFactorGraph.class); 

    /**
     * Parameters for the SrlFactorGraph.
     * @author mgormley
     */
    public static class SrlFactorGraphPrm {
        
        /** The structure of the Role variables. */
        public RoleStructure roleStructure = RoleStructure.ALL_PAIRS;
        
        /**
         * Whether the Role variables (if any) that correspond to predicates not
         * marked with a "Y" should be latent, as opposed to predicted
         * variables.
         */
        public boolean makeUnknownPredRolesLatent = true;
        
        /** The type of the link variables. */
        public VarType linkVarType = VarType.LATENT;
        
        /**
         * Whether to include a global factor which constrains the Link
         * variables to form a projective dependency tree.
         */
        public boolean useProjDepTreeFactor = false;
        
        /** Whether to allow a predicate to assign a role to itself. (This should be turned on for English) */
        public boolean allowPredArgSelfLoops = false;
        
        /** Whether to include unary factors in the model. (Ignored if there are no Link variables.) */
        public boolean unaryFactors = true;
        
        /** Whether to always include Link variables. For testing only. */
        public boolean alwaysIncludeLinkVars = false;
        
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
        LINK_ROLE_BINARY,
        ROLE_UNARY,
        LINK_UNARY,
        SENSE_UNARY,
    }
    
    /**
     * An SRL factor, which includes its type (i.e. template).
     * @author mgormley
     */
    public static class SrlFactor extends ExpFamFactor {

        SrlFactorTemplate type;
        
        public SrlFactor(VarSet vars, SrlFactorTemplate type) {
            super(vars, type);
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
        public SrlFactor(VarSet vars, SrlFactorTemplate type, Object templateKey) {
            super(vars, templateKey);
            this.type = type;
        }        
        
        public SrlFactorTemplate getFactorType() {
            return type;
        }
        
    }
    
    /**
     * Role variable.
     * 
     * @author mgormley
     */
    public static class RoleVar extends Var {
        
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
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    private RoleVar[][] roleVars;
    private SenseVar[] senseVars;

    // The sentence length.
    private final int n;
                

    public SrlFactorGraph(SrlFactorGraphPrm prm, CoNLL09Sentence sent, Set<Integer> knownPreds, CorpusStatistics cs) {
        this(prm, sent.getWords(), sent.getPlemmas(), knownPreds, cs.roleStateNames, cs.predSenseListMap);
    }

    public SrlFactorGraph(SrlFactorGraphPrm prm, List<String> words, List<String> lemmas, Set<Integer> knownPreds,
            List<String> roleStateNames, Map<String,List<String>> psMap) {
        this.prm = prm;
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
        
        // Create the Link variables.
        if (prm.useProjDepTreeFactor && prm.linkVarType != VarType.OBSERVED) {
            log.trace("Adding projective dependency tree global factor.");
            ProjDepTreeFactor treeFactor = new ProjDepTreeFactor(n, prm.linkVarType);
            rootVars = treeFactor.getRootVars();
            childVars = treeFactor.getChildVars();
            // Add the global factor.
            addFactor(treeFactor);
        } else if (prm.linkVarType == VarType.OBSERVED || prm.alwaysIncludeLinkVars) {
            log.trace("Adding observed Link variables, without the global factor.");
            rootVars = new LinkVar[n];
            childVars = new LinkVar[n][n];
            for (int i = -1; i < n; i++) {
                for (int j = 0; j < n;j++) {
                    if (prm.linkVarType == VarType.OBSERVED && (i == -1 || roleVars[i][j] == null)) {
                        // Don't add observed Link vars when the corresponding
                        // Role var doesn't exist.
                        continue;
                    }
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
            // Add the unary factors for the sense variables.
            if (prm.predictSense && i >= 0 && senseVars[i] != null && senseVars[i].getType() != VarType.OBSERVED) {
                // The template key must include the lemma appended, so that
                // there is a unique set of model parameters for each predicate.
                String templateKey = SrlFactorTemplate.SENSE_UNARY + "_" + lemmas.get(i);
                // If we've never seen this predicate, just give it to the (untrained) unknown classifier.
                if (psMap.get(lemmas.get(i)) == null) {
                    templateKey = TEMPLATE_KEY_FOR_UNKNOWN_SENSE;
                }
                addFactor(new SrlFactor(new VarSet(senseVars[i]), SrlFactorTemplate.SENSE_UNARY, templateKey));
            }
            // Add the role/link factors.
            for (int j = 0; j < n; j++) {
                if (i == -1) {
                    // Add unary factors on child Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && rootVars[j] != null) {
                        addFactor(new SrlFactor(new VarSet(rootVars[j]), SrlFactorTemplate.LINK_UNARY));
                    }
                } else {
                    // Add unary factors on Roles.
                    if (prm.unaryFactors && roleVars[i][j] != null) {
                        addFactor(new SrlFactor(new VarSet(roleVars[i][j]), SrlFactorTemplate.ROLE_UNARY));
                    }
                    // Add unary factors on child Links
                    if (prm.unaryFactors && prm.linkVarType != VarType.OBSERVED && childVars[i][j] != null) {
                        addFactor(new SrlFactor(new VarSet(childVars[i][j]), SrlFactorTemplate.LINK_UNARY));
                    }
                    // Add binary factors between Roles and Links.
                    if (roleVars[i][j] != null && childVars[i][j] != null) {
                        addFactor(new SrlFactor(new VarSet(roleVars[i][j], childVars[i][j]), SrlFactorTemplate.LINK_ROLE_BINARY));
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

    private LinkVar createLinkVar(int parent, int child) {
        String linkVarName = LinkVar.getDefaultName(parent,  child);
        return new LinkVar(prm.linkVarType, linkVarName, parent, child);
    }
    
    private SenseVar createSenseVar(int parent, List<String> senseStateNames) {
        String senseVarName = "Sense_" + parent;
        return new SenseVar(VarType.PREDICTED, senseStateNames.size(), senseVarName, senseStateNames, parent);            
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

}
