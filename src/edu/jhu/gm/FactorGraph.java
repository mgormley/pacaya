package edu.jhu.gm;

import java.util.ArrayList;
import java.util.HashSet;

import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.FactorGraph.FgNode;

/**
 * Factor graph.
 * 
 * A factor graph is an undirected bipartite graph, where each node is either a
 * factor or a variable. Internally this graph is represented by a directed
 * graph over nodes and edges, which allows a one-to-one correspondence between
 * edges and messages sent on those edges for belief propagation algorithms.
 *  
 * @author mgormley
 * 
 */
// TODO: implements BipartiteGraph<Factor,Var> 
public class FactorGraph extends DirectedGraph<FgNode, FgEdge> {
   
    /** 
     * A (directed) edge in a factor graph.
     * 
     * @author mgormley
     *
     */
    public class FgEdge extends DirectedGraph<FgNode,FgEdge>.Edge {
        
        private boolean isVarToFactor;
        private int id;
        
        public FgEdge(FgNode parent, FgNode child) {
            super(parent, child);
            if (parent.isVar()) {
                isVarToFactor = true;
            } else {
                isVarToFactor = false;
            }
        }

        public Factor getFactor() {
            if (isVarToFactor) {
                return getChild().factor;
            } else {
                return getParent().factor;
            }
        }
        
        public Var getVar() {
            if (isVarToFactor) {
                return getParent().var;
            } else {
                return getChild().var;
            }
        }

        /** Gets the unique id for this edge. */
        public int getId() {
            return id;
        }

        /**
         * Returns true if this edge is from a variable to a factor. Returns
         * false if this is from a factor to a variable.
         */
        public boolean isVarToFactor() {
            return isVarToFactor;
        }
        
    }
            
    /** 
     * A node in a factor graph.
     * 
     * @author mgormley
     *
     */
    public class FgNode extends DirectedGraph<FgNode,FgEdge>.Node {

        private boolean isVar;
        private Var var;
        private Factor factor;
        
        public FgNode(Var var) {
            super();
            this.var = var;
            isVar = true;
        }

        public FgNode(Factor factor) {
            super();
            this.factor = factor;
            isVar = false;
        }
        
        public boolean isVar() {
            return isVar;
        }
        
        public boolean isFactor() {
            return !isVar;
        }
    }
    
    /** The factors in this factor graph. */
    private ArrayList<Factor> factors;
    /** The variables in this factor graph. */
    private ArrayList<Var> vars;
    /**
     * A set of the variables in this factor graph. This is used only during
     * construction of the factor graph. At all other times, we defer to the
     * list of variables in <code>vars</code>.
     */
    private HashSet<Var> varSet;
    /**
     * Internal list of factor nodes allowing for fast lookups of nodes. It is
     * always true that factors.get(i) corresponds to the node
     * factorNodes.get(i).
     */
    private ArrayList<FgNode> factorNodes;
    /**
     * Internal list of variable nodes allowing for fast lookups of nodes. It is
     * always true that vars.get(i) corresponds to the node varNodes.get(i).
     */
    private ArrayList<FgNode> varNodes;

    public FactorGraph() {
        super();
        factors = new ArrayList<Factor>();
        vars = new ArrayList<Var>();  
        varSet = new HashSet<Var>();
        factorNodes = new ArrayList<FgNode>();
        varNodes = new ArrayList<FgNode>();            
    }
        
    public FgNode getNode(Var var) {
        return varNodes.get(var.getNodeId());
    }

    public FgNode getNode(Factor factor) {
        return factorNodes.get(factor.getNodeId());
    }

    public Var getVar(int varId) {
        return vars.get(varId);
    }

    public Factor getFactor(int factorId) {
        return factors.get(factorId);
    }

    /**
     * Adds the factor to this factor graph, additionally adding any variables
     * in its VarSet which have not already been added.
     */
    public void addFactor(Factor factor) {
        // Add the factor.
        factor.setNodeId(factors.size());
        factors.add(factor);
        FgNode fnode = new FgNode(factor);
        factorNodes.add(fnode);
        super.add(fnode);
        
        for (Var var : factor.getVars()) {
            // Add the variable.
            addVar(var);
            FgNode vnode = varNodes.get(var.getNodeId());
            // Add a directed edge between the factor and the variable.
            super.add(new FgEdge(fnode, vnode));
            // Add a directed edge between the variable and the factor.
            super.add(new FgEdge(vnode, fnode));
        }
    }

    /**
     * Adds a variable to this factor graph.
     * 
     * @param var
     *            The variable to add.
     * @return Whether or not the factor graph changed as a result of adding
     *         this variable.
     */
    public boolean addVar(Var var) {
        if (varSet.add(var)) {
            // Variable was not yet in the factor graph.
            var.setNodeId(vars.size());
            vars.add(var);
            assert(varSet.size() == vars.size());
            FgNode vnode = new FgNode(var);
            varNodes.add(vnode);
            super.add(vnode);
            return true;
        } else {
            // Variable was already in this factor graph.
            return false;
        }
    }

    /** Gets the number of factors in this factor graph. */
    public int getNumFactors() {
        return factors.size();
    }

    /** Gets the number of variables in this factor graph. */
    public int getNumVars() {
        return vars.size();
    }
    
}
