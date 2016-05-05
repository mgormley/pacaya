package edu.jhu.pacaya.sch.graph;

import static edu.jhu.pacaya.sch.graph.DiEdge.edge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import edu.jhu.pacaya.sch.util.DefaultDict;
import edu.jhu.pacaya.sch.util.OrderedSet;

/**
 * A simple directed graph where the nodes are integers
 *
 */
public class IntDiGraph {

    /**
     * maintains the node with the highest key
     */
    private int maxNode;
    
    /**
     * The nodes of the graph
     */
    private OrderedSet<Integer> nodes;

    /**
     * The edges of the graph
     */
    private OrderedSet<DiEdge> edges;
    
    /**
     * out-neighbors
     */
    private DefaultDict<Integer, List<Integer>> successors;

    /**
     * in-neighbors
     */
    private DefaultDict<Integer, List<Integer>> predecessors;
    
    public IntDiGraph() {
        nodes = new OrderedSet<>();
        edges = new OrderedSet<>();
        predecessors = new DefaultDict<>(defaultNeighbors);
        successors = new DefaultDict<>(defaultNeighbors);
        maxNode = -1;
    }
    
    public List<Integer> getSuccessors(int i) {
        return successors.getOrDefault(i, Collections.emptyList());
    }

    public List<Integer> getPredecessors(int j) {
        return predecessors.getOrDefault(j, Collections.emptyList());
    }

    /**
     * Function to begin a new list of neighbors
     */
    public static Function<Integer, List<Integer>> defaultNeighbors = i -> new LinkedList<>();
    
    /**
     * Add the edge (and the nodes) to the graph if they aren't already there
     * @param s
     * @param t
     */
    public void addEdge(int s, int t) {
        DiEdge newEdge = new DiEdge(s, t);
        if (!edges.contains(newEdge)) {
            addNode(s);
            addNode(t);
            edges.add(newEdge);
            predecessors.get(t).add(s);
            successors.get(s).add(t);
        }
    }

    public int index(Integer node) {
        return nodes.indexOf(node);
    }

    public void addNodesFrom(Collection<Integer> c) {
        for (int i : c) {
            addNode(i);
        }
    }

    public void addEdgesFrom(Collection<DiEdge> c) {
        for (DiEdge e : c) {
            addEdge(e.get1(), e.get2());
        }
    }

    public void addNode(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("node ids must be positive");
        }
        if (i > maxNode) {
            maxNode = i;
        }
        nodes.add(i);
    }

    public OrderedSet<Integer> getNodes() {
        return nodes;
    }

    public static IntDiGraph simpleGraph() {
        IntDiGraph g = new IntDiGraph();
        // add nodes in a particular order
        g.addNodesFrom(Arrays.asList(0,1,2,3));
        g.addEdgesFrom(Arrays.asList(
                edge(0, 1),
                edge(0, 2),
                edge(1, 3),
                edge(2, 3),
                edge(3, 0)
        ));
        return g;        
    }

    public static IntDiGraph simpleGraphWithStart() {
        IntDiGraph g = simpleGraph();
        g.addEdgesFrom(Arrays.asList(
            edge(4, 0),
            edge(4, 3)));
        return g;
    }

    public int max() {
        return maxNode;
    }
    
    public OrderedSet<DiEdge> getEdges() {
        return edges;
    }

    /**
     * @return true if the edge from s to t is in the edgeset
     */
//    public boolean containsEdge(int s, int t) {
//        return edges.contains(new DiEdge(s, t));
//    }

    
}
