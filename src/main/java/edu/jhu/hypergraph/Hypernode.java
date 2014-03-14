package edu.jhu.hypergraph;

public interface Hypernode {

    /** Gets a name for this node. */
    public String getLabel();
    
    /** Gets a unique identifier for this node within the hypergraph. */
    public int getId();
    
}
