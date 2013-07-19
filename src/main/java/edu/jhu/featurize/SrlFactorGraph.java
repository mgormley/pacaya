package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;

public class SrlFactorGraph extends FactorGraph {

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
        public boolean useProjDepTreeGlobalFactor = false;
    }

    public enum RoleStructure {
        /** Defines Role variables each of the "known" predicates with all possible arguments. */
        PREDS_GIVEN,
        /** The N**2 model. */
        ALL_PAIRS,
    }
    
    public enum SrlFactorTemplate {
        LINK_ROLE,
        ROLE_UNARY,
        LINK_UNARY,
    }
    
    public static class SrlFactor extends Factor {

        private SrlFactorTemplate template;

        public SrlFactor(VarSet vars, SrlFactorTemplate template) {
            super(vars);
            this.template = template;
        }
        
        public SrlFactorTemplate getFactorType() {
            return this.template;
        }
        
    }
    
    /**
     * Link variable. When true it indicates that there is an edge between its
     * parent and child.
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

    // Parameters for constructing the factor graph.
    private SrlFactorGraphPrm prm;

    // Cache of the variables for this factor graph. These arrays may contain
    // null for variables we didn't include in the model.
    private LinkVar[][] linkVars;
    private RoleVar[][] roleVars;
                
    public SrlFactorGraph(SrlFactorGraphPrm prm, CoNLL09Sentence sent, Set<Integer> knownPreds, CorpusStatistics cs) {
        super();
        this.prm = prm;
        
        final int n = sent.size();
        
        // Create the Role variables.
        roleVars = new RoleVar[n][n];
        if (prm.roleStructure == RoleStructure.PREDS_GIVEN) {
            // CoNLL-friendly model; preds given
            for (int i : knownPreds) {
                for (int j = 0; j < sent.size();j++) {
                    roleVars[i][j] = createRoleVar(i, j, knownPreds, cs);
                }
            }
        } else if (prm.roleStructure == RoleStructure.ALL_PAIRS) {
            // n**2 model
            for (int i = 0; i < sent.size(); i++) {
                for (int j = 0; j < sent.size();j++) {
                    roleVars[i][j] = createRoleVar(i, j, knownPreds, cs);
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported model structure: " + prm.roleStructure);
        }
        
        // Create the Link variables.
        linkVars = new LinkVar[n][n];
        if (prm.useProjDepTreeGlobalFactor) {

        } else {
            for (int i = 0; i < sent.size(); i++) {
                for (int j = 0; j < sent.size();j++) {
                    linkVars[i][j] = createLinkVar(i, j);
                }
            }
        }
    }

    // ----------------- Creating Variables -----------------

    private RoleVar createRoleVar(int parent, int child, Set<Integer> knownPreds, CorpusStatistics cs) {
        RoleVar roleVar;
        String roleVarName = "Role_" + parent + "_" + child;
        if (!prm.makeUnknownPredRolesLatent || knownPreds.contains((Integer) parent)) {
            roleVar = new RoleVar(VarType.PREDICTED, cs.roleStateNames.size(), roleVarName, cs.roleStateNames, parent, child);            
        } else {
            roleVar = new RoleVar(VarType.LATENT, 0, roleVarName, cs.roleStateNames, parent, child);
        }
        return roleVar;
    }

    private LinkVar createLinkVar(int parent, int child) {
        String linkVarName = LinkVar.getDefaultName(parent,  child);
        return new LinkVar(prm.linkVarType, linkVarName, parent, child);
    }
    
    // ----------------- Public Getters -----------------

    /**
     * Gets a Link variable.
     * @param i The parent position.
     * @param j The child position.
     * @return The link variable or null if it doesn't exist.
     */
    public LinkVar getLinkVar(int i, int j) {
        if (0 <= i && i < linkVars.length && 0 <= j && j < linkVars[i].length) {
            return linkVars[i][j];
        } else {
            return null;
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

}
