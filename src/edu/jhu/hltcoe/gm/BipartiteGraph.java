package edu.jhu.hltcoe.gm;

import java.util.ArrayList;

/**
 * Undirected bipartite graph.
 * 
 * @author mgormley
 *
 */
public class BipartiteGraph {

    public static class Node {
        private ArrayList<Edge> edges;
    }
    
    public static class Edge {
        private Node n1;
        private Node n2;
    }
    
    private ArrayList<Node> nodes;
    private ArrayList<Edge> edges;
    
    
    
}
