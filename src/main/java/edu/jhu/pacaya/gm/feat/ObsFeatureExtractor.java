package edu.jhu.pacaya.gm.feat;

import edu.jhu.pacaya.gm.data.UFgExample;

public interface ObsFeatureExtractor {

    /**
     * Initializes the feature extractor. This method must be called exactly
     * once before any calls to calcObsFeatureVector are made.
     * 
     * @param ex The factor graph example.
     * @param fts The templates.
     */
    void init(UFgExample ex, FactorTemplateList fts);

    /**
     * Creates the observation function feature vector for the specified factor.
     * 
     * @param factorId The id of the factor.
     * @return The feature vector on the observations only.
     */
    FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor);
    
}
