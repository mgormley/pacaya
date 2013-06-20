package edu.jhu.gm;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.BipartiteGraph.Edge;
import edu.jhu.gm.BipartiteGraph.Node;

/**
 * Factor graph.
 * 
 * @author mgormley
 *
 */
public class FactorGraph {
   
    /** 
     * An edge in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class FgEdge extends Edge {
        
        // TODO: write hashCode and equals.
        public FgEdge() {
            // TODO: 
        }
        
        public Factor getFactor() {
            Node n1 = this.getParent();
            Node n2 = this.getChild();
            Factor factor;
            Var var;
            if (n1 instanceof Factor) {
                factor = (Factor) n1;
                var = (Var) n2;
            } else {
                factor = (Factor) n2;
                var = (Var) n1;
            }
            return factor;
        }
        
        public Var getVar() {
            Node n1 = this.getParent();
            Node n2 = this.getChild();
            Factor factor;
            Var var;
            if (n1 instanceof Factor) {
                factor = (Factor) n1;
                var = (Var) n2;
            } else {
                factor = (Factor) n2;
                var = (Var) n1;
            }
            return var;
        }

        public int getId() {
            // TODO Auto-generated method stub
            return 0;
        }

        public boolean isVarToFactor() {
            // TODO Auto-generated method stub
            return false;
        }
        
    }
    
    private BipartiteGraph graph;
    private ArrayList<Factor> factors;
    private ArrayList<Var> vars;
    
    public List<FgEdge> getEdges() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getNumEdges() {
        // TODO Auto-generated method stub
        return 0;
    }

    public FgEdge getEdge(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<FgEdge> getEdges(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getNode(Var var) {
        // TODO Auto-generated method stub
        return null;
    }

    public Node getNode(Factor factor) {
        // TODO Auto-generated method stub
        return null;
    }

    public Var getVar(int varId) {
        return vars.get(varId);
    }

    public Factor getFactor(int factorId) {
        return factors.get(factorId);
    }
    
}
