package edu.jhu.parse.dep.edmonds;

/**
 * Simple edge representation.
 * 
 * @author eraldo
 * 
 */
public class SimpleWeightedEdge implements Comparable<SimpleWeightedEdge> {

    /**
     * Outgoing node.
     */
    public final int from;

    /**
     * Incoming node.
     */
    public final int to;

    /**
     * Edge weight.
     */
    public double weight;

    /**
     * Constructor.
     * 
     * @param from
     * @param to
     * @param weight
     */
    public SimpleWeightedEdge(int from, int to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    @Override
        public int compareTo(SimpleWeightedEdge o) {
        if (weight < o.weight)
            return 1;
        if (weight > o.weight)
            return -1;
        return 0;
    }

}