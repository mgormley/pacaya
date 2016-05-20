package edu.jhu.pacaya.gm.model;

import edu.jhu.pacaya.gm.feat.FeatureExtractor;
import edu.jhu.pacaya.gm.feat.FeatureVector;

/**
 * An exponential family factor which takes a FeatureExtractor at construction time and 
 * uses it to extract features.
 * 
 * @author mgormley
 */
public class FeExpFamFactor extends ExpFamFactor {

    private static final long serialVersionUID = 1L;
    private FeatureExtractor fe;
    // Feature cache.
    private FeatureVector[] features;

    public FeExpFamFactor(VarSet vars, FeatureExtractor fe) {
        super(vars);
        this.fe = fe;
        features = new FeatureVector[vars.calcNumConfigs()];
    }

    @Override
    public FeatureVector getFeatures(int configId) {
        if (features[configId] == null) {
            features[configId] = fe.calcFeatureVector(this, configId);
        }
        return features[configId];
    }
    
}
