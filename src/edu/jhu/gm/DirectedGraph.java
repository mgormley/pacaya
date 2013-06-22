package edu.jhu.gm;

import java.util.ArrayList;
import java.util.List;

/**
 * Undirected bipartite graph.
 * 
 * @author mgormley
 *
 */
public class DirectedGraph<N extends DirectedGraph<N,E>.Node, E extends DirectedGraph<N,E>.Edge> {

    public class Node {
        private ArrayList<E> inEdges;
        private ArrayList<E> outEdges;
        /** Whether this node has been added to the graph. */
        private boolean added;
        public Node() {
            added = false;
            inEdges = new ArrayList<E>();
            outEdges = new ArrayList<E>();
        }
        public List<E> getInEdges() {
            return inEdges;
        }
        public List<E> getOutEdges() {
            return outEdges;
        }
    }
    
    public class Edge {
        private N n1;
        private N n2;
        @SuppressWarnings("unchecked")
        public Edge(N parent, N child) {
            this.n1 = parent;
            this.n2 = child;
            this.n1.outEdges.add((E) this);
            this.n2.inEdges.add((E) this);
        }
        /** This can also be thought of as the node sending the messages. */
        public N getParent() {
            return n1;
        }
        /** This can also be thought of as the node receiving the messages. */
        public N getChild() {
            return n2;
        }
    }
    
    private ArrayList<N> nodes;
    private ArrayList<E> edges;
    
    public DirectedGraph() {
        nodes = new ArrayList<N>();
        edges = new ArrayList<E>();
    }

    /** Adds the node if it's not already present in the graph. */
    public void add(N node) {
        if (! node.added) {
            nodes.add(node);
            node.added = true;
        }
    }
    
    /** Adds the edge and its nodes. */
    public void add(E edge) {
        edges.add(edge);
        add(edge.getChild());
        add(edge.getParent());
    }
    
    public List<E> getEdges() {
        return edges;
    }

    public ArrayList<N> getNodes() {
        return nodes;
    }    
    
    public int getNumEdges() {
        return edges.size();
    }
    
    public int getNumNodes() {
        return nodes.size();
    }
    
    public N getNode(int i) {
        return nodes.get(i);
    }
    
    public E getEdge(int i) {
        return edges.get(i);
    }
    
}
