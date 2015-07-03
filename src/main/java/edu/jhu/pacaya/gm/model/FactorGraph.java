package edu.jhu.pacaya.gm.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.util.BipartiteGraph;
import edu.jhu.pacaya.gm.util.EdgeList;
import edu.jhu.prim.list.IntArrayList;

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
public class FactorGraph implements Serializable {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(FactorGraph.class);
    
    /** The factors in this factor graph. */
    private ArrayList<Factor> factors;
    /** The variables in this factor graph. */
    private ArrayList<Var> vars;
    private int numUndirEdges = 0;
    
    private BipartiteGraph<Var,Factor> bg;
    
    public FactorGraph() {
        super();
        factors = new ArrayList<Factor>();
        vars = new ArrayList<Var>();
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
    public void addFactor(Factor factor) {
        int id = factor.getId();
        boolean alreadyAdded = (0 <= id && id < factors.size());
        if (alreadyAdded) {
            if (factors.get(id) != factor) {
                throw new IllegalStateException("Factor id already set, but factor not yet added.");
            }
        } else {            
            // Factor was not yet in the factor graph.
            //
            // Check and set the id.
            if (id != -1 && id != factors.size()) {
                throw new IllegalStateException("Factor id already set, but incorrect: " + id);
            }
            factor.setId(factors.size());
            // Add the factor.
            factors.add(factor);
            
            // Add each variable...
            for (Var var : factor.getVars()) {
                // Add the variable.
                addVar(var);
                numUndirEdges++;
            }

            if (bg != null) { log.warn("Discarding BipartiteGraph. This may indicate inefficiency."); }
            bg = null;
        }
    }

    /**
     * Adds a variable to this factor graph, if not already present.
     * 
     * @param var The variable to add.
     * @return The node for this variable.
     */
    public void addVar(Var var) {
        int id = var.getId();
        boolean alreadyAdded = (0 <= id && id < vars.size());
        if (alreadyAdded) {
            if (vars.get(id) != var) {
                throw new IllegalStateException("Var id already set, but factor not yet added.");
            }
        } else {  
            // Var was not yet in the factor graph.
            //
            // Check and set the id.
            if (id != -1 && id != vars.size()) {
                throw new IllegalStateException("Var id already set, but incorrect: " + id);
            }
            var.setId(vars.size());
            // Add the Var.
            vars.add(var);

            if (bg != null) { log.warn("Discarding BipartiteGraph. This may indicate inefficiency."); }
            bg = null;
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

    public List<Factor> getFactors() {
        return Collections.unmodifiableList(factors);
    }
    
    public List<Var> getVars() {
        return Collections.unmodifiableList(vars);
    }
    
    public void updateFromModel(FgModel model) {
        for (int a=0; a < this.getNumFactors(); a++) {
            Factor f = this.getFactor(a);
            f.updateFromModel(model);
        }
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();        
        for (int e=0; e<getNumEdges(); e++) {
            sb.append(edgeToString(e));
            sb.append("\n");
        }
        return sb.toString();
    }

    public String edgeToString(int e) {
        BipartiteGraph<Var, Factor> bg = getBipgraph();
        String var = String.format("Var[%s]", bg.t1E(e).getName());
        String factor = String.format("Factor[%s]", varsToString(bg.t2E(e).getVars()));
        String parent = bg.isT1T2(e) ? var : factor;
        String child = bg.isT1T2(e) ? factor : var;                
        return String.format("FgEdge [%s --> %s]", parent, child);
    }

    public static String varsToString(VarSet vars) {
        StringBuilder sb = new StringBuilder();
        for (Var var : vars) {
            sb.append(var.getName());
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
    
    public int getNumEdges() {
        return numUndirEdges * 2;
    }
    
    public int getNumUndirEdges() {
        return numUndirEdges;
    }
    
    public BipartiteGraph<Var,Factor> getBipgraph() {
        if (bg == null) {
            int numUndirEdges = 0;
            for (Factor f : factors) {
                numUndirEdges += f.getVars().size();
            }
            EdgeList el = new EdgeList(numUndirEdges);
            for (Factor f : factors) {
                for (Var v : f.getVars()) {
                    assert v.getId() < vars.size();
                    assert f.getId() < factors.size();
                    el.addEdge(v.getId(), f.getId());
                }
            }
            bg = new BipartiteGraph<>(vars, factors, el);
        }
        return bg;
    }
    
    public IntArrayList getConnectedComponents() {
        return getBipgraph().getConnectedComponentsT2();
    }
    
}
