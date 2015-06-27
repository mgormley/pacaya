package edu.jhu.pacaya.gm.data;

import java.io.Serializable;

import edu.jhu.pacaya.gm.model.VarConfig;

/**
 * Factor graph example.
 * 
 * @author mgormley
 */
public interface LFgExample extends UFgExample, Serializable {
    
    /** Gets the gold configuration of the variables. */
    public VarConfig getGoldConfig();
    
    /** Gets the weight of this example for use in log-liklihood training. */
    public double getWeight();

    /** Gets the gold configuration of the predicted variables ONLY for the given factor. */ 
    public VarConfig getGoldConfigPred(int factorId);
    
    /** Gets the gold configuration index of the predicted variables ONLY for the given factor. */
    public int getGoldConfigIdxPred(int factorId);
    
}
