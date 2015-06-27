package edu.jhu.pacaya.gm.util;

import java.util.ArrayList;

/**
 * Undirected bipartite graph.
 * 
 * @author mgormley
 *
 */
public class BipartiteGraph<T1, T2> {
    
    /** Nodes of type 1. */
    private ArrayList<T1> nodes1;
    /** Nodes of type 2. */
    private ArrayList<T2> nodes2;
    
    /** Neighbors of type 1 nodes. (indexed by position of node in nodes1 list.) */
    private int[][] nbs1;
    /** Neighbors of type 2 nodes. (indexed by position of node in nodes2 list.) */
    private int[][] nbs2;
        
    //ArrayList<T2> getNeighborsOfType1(T1 node);
    //ArrayList<T1> getNeighborsOfType2(T2 node);
    
    
}
