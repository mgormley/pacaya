package edu.jhu.hypergraph;

public interface Hyperpotential {

    /** Gets the potential for an edge in a hypergraph. */
    double getScore(Hyperedge e);
    
}
