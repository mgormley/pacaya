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
        ArrayList<E> inEdges;
        ArrayList<E> outEdges;
        /** Whether this node has been added to the graph. */
        boolean added;
        boolean marked;
        public Node() {
            added = false;
            inEdges = new ArrayList<E>();
            outEdges = new ArrayList<E>();
            marked = false;
        }
        /** Gets the edges from another node to this one. */
        public List<E> getInEdges() {
            return inEdges;
        }
        /** Gets the edges from this node to another. */
        public List<E> getOutEdges() {
            return outEdges;
        } 
        /** Whether this node is marked. */
        public boolean isMarked() {
            return marked;
        }
        /** Sets whether this node is marked. */
        public void setMarked(boolean marked) {
            this.marked = marked;
        }
    }
    
    public class Edge {
        N n1;
        N n2;
        boolean added;
        boolean marked;
        @SuppressWarnings("unchecked")
        public Edge(N parent, N child) {
            this.added = false;
            this.n1 = parent;
            this.n2 = child;
            this.n1.outEdges.add((E) this);
            this.n2.inEdges.add((E) this);
            this.marked = false;
        }
        /** This can also be thought of as the node sending the messages. */
        public N getParent() {
            return n1;
        }
        /** This can also be thought of as the node receiving the messages. */
        public N getChild() {
            return n2;
        }        
        /** Whether this edge is marked. */
        public boolean isMarked() {
            return marked;
        }

        /** Sets whether this edge is marked. */
        public void setMarked(boolean marked) {
            this.marked = marked;
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
    
    /** Adds the edge and its nodes if not already present in the graph. */
    public void add(E edge) {
        if (! edge.added) { 
            edges.add(edge);
            edge.added = true;
            add(edge.getChild());
            add(edge.getParent());
        }
    }

    /** Removes an edge if it was in the graph. Does not remove its parent/child nodes. */
    public void remove(E edge) {
        if (! edge.added){
            return;
        }
        boolean contained;         
        contained = edges.remove(edge);
        assert(contained);
        contained = edge.getChild().inEdges.remove(edge);
        assert(contained);
        contained = edge.getParent().outEdges.remove(edge);
        assert(contained);
        
        edge.added = false;
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
    
    /**
     * Returns whether or not the given node is the root of a tree in this directed graph. 
     */
    public boolean isTree(N node) {
        setMarkedAllNodes(false);
        return isTreeRecurse(node);
    }
    
    private boolean isTreeRecurse(N node) {        
        node.setMarked(true);
        if (node.getInEdges().size() > 1) {
            return false;
        }
        for (E e : node.getOutEdges()) {
            N n = e.getChild();
            if (n.isMarked()) {
                return false;
            }
            if (!isTreeRecurse(n)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Gets the connected components of the graph.
     * @return A list containing an arbitrary node in each each connected component.
     */
    public List<N> getConnectedComponents() {
        setMarkedAllNodes(false);
        ArrayList<N> roots = new ArrayList<N>();
        //for (int i=0; i<nodes.size(); i++) {
        for (N n : nodes) {
            if (!n.isMarked()) {
                roots.add(n);
                dfs(n);
            }
        }
        return roots;
    }

    /**
     * Runs depth-first search on the graph starting at node n, marking each node as it is encountered.
     * @param root
     */
    private void dfs(Node root) {
        root.setMarked(true);
        for (Edge e : root.getOutEdges()) {
            N n = e.getChild();
            if (!n.isMarked()) {
                dfs(n);
            }
        }
    }

    /**
     * Calls setMarked(marked) on all the nodes.
     */
    public void setMarkedAllNodes(boolean marked) {
        for (Node n : nodes) {
            n.setMarked(marked);
        }
    }
    
    /**
     * Calls setMarked(marked) on all the edges.
     */
    public void setMarkedAllEdges(boolean marked) {
        for (Edge e : edges) {
            e.setMarked(marked);
        }
    }
    
}
