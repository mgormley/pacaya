package edu.jhu.hypergraph;

import edu.jhu.util.semiring.Semiring;

public interface Hyperpotential {

    /** Gets the potential for an edge in a hypergraph. */
    double getScore(Hyperedge e, Semiring s);
    
}
