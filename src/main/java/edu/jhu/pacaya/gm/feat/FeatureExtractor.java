package edu.jhu.pacaya.gm.feat;

import edu.jhu.pacaya.gm.model.FeExpFamFactor;



public interface FeatureExtractor {

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
