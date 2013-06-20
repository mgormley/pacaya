package edu.jhu.gm;

import java.util.ArrayList;

import edu.jhu.gm.BipartiteGraph.Node;

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
        /** This can also be thought of as the node sending the messages. */
        public Node getParent() {
            return n1;
        }
        /** This can also be thought of as the node receiving the messages. */
        public Node getChild() {
            return n2;
        }
    }
    
    private ArrayList<Node> nodes;
    private ArrayList<Edge> edges;
    
    
    
}
