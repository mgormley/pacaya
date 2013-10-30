package edu.jhu.gm.feat;



public interface FeatureExtractor {

    /**
     * Creates the feature vector for the specified factor, given the
     * configurations for all the variables.
     * 
     * @param factorId The id of the factor.
     * @param configId The configuration id of the latent and predicted variables ONLY.
     * @return The feature vector.
     */
    FeatureVector calcFeatureVector(int factorId, int configId);

}
