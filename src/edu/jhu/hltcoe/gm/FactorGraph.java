package edu.jhu.hltcoe.gm;

import java.util.ArrayList;

import edu.jhu.hltcoe.gm.BipartiteGraph.Node;

/**
 * Factor graph.
 * 
 * @author mgormley
 *
 */
public class FactorGraph {
    
    /**
     * A factor in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class Factor extends Node {
        
        /** The set of variables in this factor. */
        //TODO:private VarSet vars;
        
        
    }
    
    /**
     * A variable in a factor graph.
     * 
     * @author mgormley
     *
     */
    public static class Var extends Node {
        
        /** Whether this variable is observed or not. */
        private boolean isObserved;
        
    }
    
    private BipartiteGraph graph;
    private ArrayList<Factor> factors;
    private ArrayList<Var> vars;
    
}
