package edu.jhu.gm;

public interface CrfFeatureExtractor {

    /**
     * Creates the observation function feature vector for the specified factor.
     * 
     * @param factorId The id of the factor.
     * @return The feature vector on the observations only.
     */
    FeatureVector calcObsFeatureVector(int factorId);

}
