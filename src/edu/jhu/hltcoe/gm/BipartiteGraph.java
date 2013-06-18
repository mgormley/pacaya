package edu.jhu.hltcoe.gm;

import java.util.ArrayList;

import edu.jhu.hltcoe.gm.BipartiteGraph.Node;

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
        public Node getFirst() {
            return n1;
        }
        public Node getSecond() {
            return n2;
        }
    }
    
    private ArrayList<Node> nodes;
    private ArrayList<Edge> edges;
    
    
    
}
