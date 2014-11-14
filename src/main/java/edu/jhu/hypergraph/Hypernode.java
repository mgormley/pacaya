package edu.jhu.hypergraph;

import java.io.Serializable;

// We implement serializable to allow for easy deep copies when unit testing.
// A Hypernode must also implement hashCode and equals.
public interface Hypernode extends Serializable {

    /** Gets a name for this node. */
    public String getLabel();
    
    /** Gets a unique identifier for this node within the hypergraph. */
    public int getId();
    
}
