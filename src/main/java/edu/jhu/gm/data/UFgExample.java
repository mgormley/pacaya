package edu.jhu.gm.data;

import java.io.Serializable;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.VarConfig;

/**
 * An unlabeled factor graph example. This class only stores the factor graph
 * and the assignment to the observed variables.
 * 
 * @author mgormley
 */
public interface UFgExample extends Serializable {

    /**
     * Gets the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     */
    public FactorGraph getFgLatPred();

    /**
     * Updates the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLatPred(FgModel model, boolean logDomain);

    public boolean hasLatentVars();

    /** Gets the original input factor graph. */
    public FactorGraph getOriginalFactorGraph();

    /** Gets the configuration of the OBSERVED variables. */
    public VarConfig getObsConfig();
    
}
