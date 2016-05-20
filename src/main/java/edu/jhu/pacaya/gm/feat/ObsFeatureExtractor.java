package edu.jhu.pacaya.gm.feat;


public interface ObsFeatureExtractor {

    /**
     * Creates the observation function feature vector for the specified factor.
     * 
     * @param factorId The id of the factor.
     * @return The feature vector on the observations only.
     */
    FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor);
    
}
