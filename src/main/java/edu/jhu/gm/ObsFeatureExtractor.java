package edu.jhu.gm;

public interface ObsFeatureExtractor {

    /**
     * Creates the observation function feature vector for the specified factor.
     * 
     * @param factorId The id of the factor.
     * @return The feature vector on the observations only.
     */
    FeatureVector calcObsFeatureVector(int factorId);

    /**
     * Initializes the feature extractor. This method will be called only once.
     * 
     * @param fg The original factor graph.
     * @param fgLat The factor graph with the predicted/observed variables clamped.
     * @param fgLatPred The factor graph with the observed variables clamped.
     * @param goldConfig The gold configuration of all variables in the factor graph.
     * @param fts The templates.
     */
    void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig, FeatureTemplateList fts);

    /** Signals the feature extractor that it can clear any cached features. */
    void clear();
    
}
