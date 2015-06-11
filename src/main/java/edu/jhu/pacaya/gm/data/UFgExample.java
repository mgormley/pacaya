package edu.jhu.pacaya.gm.data;

import java.io.Serializable;

import edu.jhu.pacaya.gm.model.FactorGraph;

/**
 * An unlabeled factor graph example. This class only stores the factor graph
 * and the assignment to the observed variables.
 * 
 * @author mgormley
 */
public interface UFgExample extends Serializable {

    /** Gets the original input factor graph. */
    public FactorGraph getFactorGraph();

    /** Returns true iff this factor graph contains latent variables. */
    public boolean hasLatentVars();
    
}
