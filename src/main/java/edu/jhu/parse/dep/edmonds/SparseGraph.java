package edu.jhu.parse.dep.edmonds;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represent a sparse, directed graph by a map of adjacency lists.
 * 
 * @author eraldo
 * 
 */
public class SparseGraph {
    /**
     * Map of adjacency lists.
     */
    private Map<Integer, Set<Integer>> edges;

    /**
     * Create an empty sparse graph.
     */
    public SparseGraph() {
        edges = new TreeMap<Integer, Set<Integer>>();
    }

    /**
     * Add an edge from node <code>from</code> to node <code>to</code>.
     * 
     * @param from
     * @param to
     */
    public void addEdge(int from, int to) {
        Set<Integer> outEdges = edges.get(from);
        if (outEdges == null) {
            outEdges = new TreeSet<Integer>();
            edges.put(from, outEdges);
        }
        outEdges.add(to);
    }

    /**
     * Return a linked list (possibly null) with the outgoing edges of node
     * <code>from</code>.
     * 
     * @param from
     * @return
     */
    public Set<Integer> getOutEdges(int from) {
        return edges.get(from);
    }

    /**
     * Return the set of adjacencies.
     * 
     * @return
     */
    public Set<Entry<Integer, Set<Integer>>> getAdjacencySet() {
        return edges.entrySet();
    }

    /**
     * Build and return a sparse graph by reversing all edges in this graph.
     * 
     * @return
     */
    public SparseGraph reversed() {
        SparseGraph reverse = new SparseGraph();
        for (Entry<Integer, Set<Integer>> entryFrom : edges.entrySet())
            for (Integer to : entryFrom.getValue())
                reverse.addEdge(to, entryFrom.getKey());
        return reverse;
    }
}