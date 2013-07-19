package edu.jhu.featurize;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.Var.VarType;

public class SrlFactorGraph extends FactorGraph {

    public enum FactorType {
        LINK_ROLE,
        ROLE_UNARY,
        LINK_UNARY,
    }
    
    public static class SrlFactor extends Factor {

        private FactorType type;

        public SrlFactor(VarSet vars, FactorType type) {
            super(vars);
            this.type = type;
        }
        
        public FactorType getFactorType() {
            return this.type;
        }
        
    }
    
    public static class LinkVar extends Var {
        
        public LinkVar(VarType type, int numStates, String name, List<String> stateNames) {
            super(type, numStates, name, stateNames);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    public static class RoleVar extends Var {

        public RoleVar(VarType type, int numStates, String name, List<String> stateNames) {
            super(type, numStates, name, stateNames);
            // TODO Auto-generated constructor stub
        }
        
    }
    
    private LinkVar[][] linkVars;
    private RoleVar[][] roleVars;
    
    
    public SrlFactorGraph(CoNLL09Sentence sent, Set<Integer> knownPreds) {
        super();
    }

    public FactorGraph getFactorGraph() {

//        if (prm.structure == ModelStructure.PREDS_GIVEN) {
//            // CoNLL-friendly model; preds given
//            for (int i : knownPreds) {
//                // senseGroup = getSenseFeatures(i, sent, srlEdges, knownPairs);
//                String pred = Integer.toString(sent.get(i).getId());
//                for (int j = 0; j < sent.size();j++) {
//                    String arg = Integer.toString(sent.get(j).getId());
//                    addRoleForWordPair(i, j, pred, arg, sent, knownPairs, knownPreds, srlEdges);
//                }
//            }
//        } else if (prm.structure == ModelStructure.ALL_PAIRS) {
//            // n**2 model
//            for (int i = 0; i < sent.size(); i++) {
//                // senseGroup = getSenseFeatures(i, sent, srlEdges, knownPairs);
//                String pred = Integer.toString(sent.get(i).getId());
//                for (int j = 0; j < sent.size();j++) {
//                    String arg = Integer.toString(sent.get(j).getId());
//                    extractFeatsAndVars(i, j, pred, arg, sent, knownPairs, knownPreds, srlEdges);                       
//                }
//            }
//        } else {
//            throw new IllegalArgumentException("Unsupported model structure: " + prm.structure);
//        }
        // TODO Auto-generated method stub
        return null;
    }


    // ----------------- Extracting Variables -----------------
    public VarConfig getVariables(int i, int j, String pred, String arg, CoNLL09Sentence sent, Map<Pair<Integer,Integer>, String> knownPairs, Set<Integer> knownPreds) {
        VarConfig vc = new VarConfig();
        // Observed input Link variables
        Var linkVar;
        String linkVarName = "Link_" + pred + "_" + arg;
        // Syntactic head, from dependency parse.
        int head = sent.get(j).getHead();
        String stateName;
        if (head != i) {
            stateName = "False";
        } else {
            stateName = "True";
        }
        linkVar = new Var(VarType.OBSERVED, cs.linkStateNames.size(), linkVarName, cs.linkStateNames);
        vc.put(linkVar, stateName);

        // Predicted Semantic roles
        Var roleVar;
        String roleVarName = "Role_" + pred + "_" + arg;
        int[] key = {i, j};
        // for training, we must know pred.
        // for testing, we don't know the pred if it's not CoNLL; 
        // but the features will be the same regardless of the state here.
        if (knownPreds.contains((Integer) i)) {
            if (knownPairs.containsKey(key)) {
                String label = knownPairs.get(key);
                stateName = label.toLowerCase();
            } else {
                stateName = "_";
            }
            roleVar = new Var(VarType.PREDICTED, cs.roleStateNames.size(), roleVarName, cs.roleStateNames);            
        } else { 
            roleVar = new Var(VarType.LATENT, 0, roleVarName, cs.roleStateNames);
        }
        vc.put(roleVar, stateName);
        return vc;
    }  
    
    public VarConfig getTrainAssignment() {
        // TODO Auto-generated method stub
        return null;
    }

    public LinkVar getLinkVar(int i, int j) {
        if (0 <= i && i < linkVars.length && 0 <= j && j < linkVars[i].length) {
            return linkVars[i][j];
        } else {
            return null;
        }
    }

    public RoleVar getRoleVar(int i, int j) {
        if (0 <= i && i < roleVars.length && 0 <= j && j < roleVars[i].length) {
            return roleVars[i][j];
        } else {
            return null;
        }
    }

}
