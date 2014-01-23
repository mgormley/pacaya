package edu.jhu.gm.feat;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.model.FeExpFamFactor;



public interface FeatureExtractor {

    /**
     * Initializes the feature extractor. This method must be called exactly
     * once before any calls to calcObsFeatureVector are made.
     * 
     * @param ex The factor graph example.
     */
    void init(FgExample ex);

    /**
     * Creates the feature vector for the specified factor, given the
     * configurations for all the variables.
     * 
     * @param feExpFamFactor The factor for which to extract features.
     * @param configId The configuration id of the latent and predicted variables ONLY.
     * @return The feature vector.
     */
    FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId);
    
}
