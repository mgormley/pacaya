package edu.jhu.gm;

import java.util.List;


public interface FeatureExtractor {

    /**
     * Creates the feature vector for the specified factor, given the
     * configurations for all the variables.
     * 
     * @param factorId The id of the factor.
     * @param varConfig The configuration of the variables for the factor in the
     *            original factor graph.
     * @return The feature vector.
     */
    FeatureVector calcFeatureVector(int factorId, VarConfig varConfig);
    
}
