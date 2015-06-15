package edu.jhu.pacaya.gm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.pacaya.gm.model.FactorGraph.FgEdge;
import edu.jhu.pacaya.gm.model.FactorGraph.FgNode;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.DirectedGraph;

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
public class FactorGraph extends DirectedGraph<FgNode, FgEdge> implements Serializable {
    
    private static final long serialVersionUID = 1L;
   
    /** 
     * A (directed) edge in a factor graph.
     * 
     * @author mgormley
     *
     */
    public class FgEdge extends DirectedGraph<FgNode,FgEdge>.Edge implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        private boolean isVarToFactor;
        private int id;
        private FgEdge opposing;
        
        public FgEdge(FgNode parent, FgNode child, int id) {
            super(parent, child);
            if (parent.isVar()) {
                isVarToFactor = true;
            } else {
                isVarToFactor = false;
            }
            this.id = id;
        }

        /** Gets the factor connected to this edge. */
        public Factor getFactor() {
            if (isVarToFactor) {
                return getChild().factor;
            } else {
                return getParent().factor;
            }
        }
        
        /** Gets the variable connected to this edge. */
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

        /** Gets the edge identical to this one, except that the parent and child are swapped. */
        public FgEdge getOpposing() {
            return opposing;
        }

        /** 
         * Sets the edge identical to this one, except that the parent and child are swapped.
         * @param opposing The opposing edge. 
         * @throws IllegalStateException If the edge is not the opposing edge. 
         */
        public void setOpposing(FgEdge opposing) {
            if (opposing.getParent() != this.getChild() || opposing.getChild() != this.getParent()) {
                throw new IllegalStateException("This is not the opposing edge: " + opposing + " for this edge: " + this);
            }
            this.opposing = opposing;
        }

        @Override
        public String toString() {
            return "FgEdge [id=" + id + ", " + edgeToString(this) +"]";
        }
        
        private String edgeToString(FgEdge edge) {
            return String.format("%s --> %s", nodeToString(edge.getParent()), nodeToString(edge.getChild()));
        }

        private String nodeToString(FgNode node) {
            if (node.isVar()) {
                return "Var[" + node.getVar().getName() + "]";
            } else {
                return "Factor[" + varsToString(node.getFactor().getVars()) + "]";
            }
        }

        private String varsToString(VarSet vars) {
            StringBuilder sb  = new StringBuilder();
            for (Var var : vars) {
                sb.append(var.getName());
                sb.append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            return sb.toString();
        }
                
    }
            
    /** 
     * A node in a factor graph.
     * 
     * @author mgormley
     *
     */
    public class FgNode extends DirectedGraph<FgNode,FgEdge>.Node implements Serializable {
        
        private static final long serialVersionUID = 1L;

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
        
        public Factor getFactor() {
            assert(isFactor());
            return factor;
        }
        
        public Var getVar() {
            assert(isVar());
            return var;
        }

        @Override
        public String toString() {
            return "FgNode [isVar=" + isVar + ", var=" + var + ", factor="
                    + factor + "]";
        }
                
    }
    
    /** The factors in this factor graph. */
    private ArrayList<Factor> factors;
    /** The variables in this factor graph. */
    private ArrayList<Var> vars;
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
        factorNodes = new ArrayList<FgNode>();
        varNodes = new ArrayList<FgNode>();
    }
    
    /**
     * Gets a new factor graph, identical to this one, except that specified variables are clamped
     * to their values. This is accomplished by adding a unary factor on each clamped variable. The
     * original K factors are preserved in order with IDs 1...K.
     * 
     * @param clampVars The variables to clamp.
     */
    public FactorGraph getClamped(VarConfig clampVars) {
        FactorGraph clmpFg = new FactorGraph();
        // Add ALL the original variables to the clamped factor graph.
        for (Var v : this.getVars()) {
            clmpFg.addVar(v);
        }
        // Add ALL the original factors to the clamped factor graph.
        for (Factor origFactor : this.getFactors()) {
//            if (origFactor instanceof GlobalFactor) {
//                // Add an empty factor.
//                clmpFg.addFactor(new ExplicitFactor(new VarSet()));
//            } else {
//                clmpFg.addFactor(origFactor);
//            }
            clmpFg.addFactor(origFactor);

        }
        // Add unary factors to the clamped variables to ensure they take on the correct value.
        for (Var v : clampVars.getVars()) {
            // TODO: We could skip these (cautiously) if there's already a
            // ClampFactor attached to this variable.
            int c = clampVars.getState(v);
            clmpFg.addFactor(new ClampFactor(v, c));
        }        
        return clmpFg;
    }
    
    public FgNode getNode(Var var) {
        return getVarNode(var.getId());
    }

    public FgNode getNode(Factor factor) {
        return getFactorNode(factor.getId());
    }

    public FgNode getVarNode(int varId) {
        return varNodes.get(varId);
    }
    
    public FgNode getFactorNode(int factorId) {
        return factorNodes.get(factorId);
    }

    public Var getVar(int varId) {
        return vars.get(varId);
    }

    public Factor getFactor(int factorId) {
        return factors.get(factorId);
    }
    
    /**
     * Adds a factor to this factor graph, if not already present, additionally adding any variables
     * in its VarSet which have not already been added.
     * 
     * @param var The factor to add.
     * @return The node for this factor.
     */
    public FgNode addFactor(Factor factor) {
        int id = factor.getId();
        boolean alreadyAdded = (0 <= id && id < factors.size());
        FgNode fnode;
        if (alreadyAdded) {
            if (factors.get(id) != factor) {
                throw new IllegalStateException("Factor id already set, but factor not yet added.");
            }
            fnode = factorNodes.get(id);
        } else {            
            // Factor was not yet in the factor graph.
            //
            // Check and set the id.
            if (id != -1 && id != factors.size()) {
                throw new IllegalStateException("Factor id already set, but incorrect: " + id);
            }
            factor.setId(factors.size());
            // Add the factor.
            fnode = new FgNode(factor);
            factors.add(factor);
            factorNodes.add(fnode);
            super.add(fnode);
            
            // Add each variable...
            for (Var var : factor.getVars()) {
                // Add the variable.
                FgNode vnode = addVar(var);
                // Add a directed edge between the factor and the variable.
                FgEdge edge1 = new FgEdge(fnode, vnode, super.getNumEdges());
                super.add(edge1);
                // Add a directed edge between the variable and the factor.
                FgEdge edge2 = new FgEdge(vnode, fnode, super.getNumEdges());
                super.add(edge2);
                
                edge1.setOpposing(edge2);
                edge2.setOpposing(edge1);
            }
        }
        return fnode;
    }

    /**
     * Adds a variable to this factor graph, if not already present.
     * 
     * @param var The variable to add.
     * @return The node for this variable.
     */
    public FgNode addVar(Var var) {
        int id = var.getId();
        boolean alreadyAdded = (0 <= id && id < vars.size());
        FgNode vnode;
        if (alreadyAdded) {
            if (vars.get(id) != var) {
                throw new IllegalStateException("Var id already set, but factor not yet added.");
            }
            vnode = varNodes.get(id);
        } else {  
            // Var was not yet in the factor graph.
            //
            // Check and set the id.
            if (id != -1 && id != vars.size()) {
                throw new IllegalStateException("Var id already set, but incorrect: " + id);
            }
            var.setId(vars.size());
            // Add the Var.
            vnode = new FgNode(var);
            vars.add(var);
            varNodes.add(vnode);
            super.add(vnode);
        }
        return vnode;
    }

    /** Gets the number of factors in this factor graph. */
    public int getNumFactors() {
        return factors.size();
    }

    /** Gets the number of variables in this factor graph. */
    public int getNumVars() {
        return vars.size();
    }

    public List<Factor> getFactors() {
        return Collections.unmodifiableList(factors);
    }
    
    public List<Var> getVars() {
        return Collections.unmodifiableList(vars);
    }
    
    /** Returns whether this factor graph consists of connected components which are all trees. */
    public boolean hasTreeComponents() {
        for (FgNode node : getConnectedComponents()) {
            if (!isUndirectedTree(node)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Returns whether or not the given node is the root of an UNDIRECTED tree in this
     * factor graph.
     */
    public boolean isUndirectedTree(FgNode node) {
        setMarkedAllNodes(false);
        return isUndirectedTreeRecurse(node, null);
    }
    
    private boolean isUndirectedTreeRecurse(FgNode node, FgEdge edgeToSkip) {
        node.setMarked(true);
        for (FgEdge e : node.getOutEdges()) {
            if (e == edgeToSkip) {
                continue;
            }
            FgNode n = e.getChild();
            if (n.isMarked()) {
                return false;
            }
            if (!isUndirectedTreeRecurse(n, e.getOpposing())) {
                return false;
            }
        }
        return true;
    }
    
    public void updateFromModel(FgModel model) {
        for (int a=0; a < this.getNumFactors(); a++) {
            Factor f = this.getFactor(a);
            f.updateFromModel(model);
        }
    }
    
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("# vars: " + vars.size() + "\n");
        sb.append("# facs: " + factors.size() + "\n");
        int numVarNbs = 0;
        for (FgNode node : varNodes) {
            numVarNbs += node.getOutEdges().size();            
        }
        sb.append("avg. neighbors of var: " + (double)numVarNbs / vars.size() + "\n");
        int numFacNbs = 0;
        for (FgNode node : factorNodes) {
            numFacNbs += node.getOutEdges().size();            
        }
        sb.append("avg. neighbors of fac: " + (double)numFacNbs / factors.size() + "\n");        
        return sb.toString();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (FgEdge e : getEdges()) {
            sb.append(e.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
}
