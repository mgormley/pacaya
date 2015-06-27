package edu.jhu.pacaya.hypergraph;

import java.util.List;

public interface Hypergraph {

    /** Function which can be applied to a hyperedge. */
    public interface HyperedgeFn {
        void apply(Hyperedge e);
    }

    /** Gets the root node of the hypergraph. */
    public Hypernode getRoot();
    
    /** Returns all the nodes in the hypergraph is some arbitrary order. */
    public List<Hypernode> getNodes();

    /** Gets the number of hyperedges. */
    public int getNumEdges();
    
    /** Applies a function to each edge in the hypergraph in topological order.  */
    public void applyTopoSort(HyperedgeFn fn);
    
    /** Applies a function to each edge in the hypergraph in reverse topological order.  */
    public void applyRevTopoSort(HyperedgeFn fn);
        
}
