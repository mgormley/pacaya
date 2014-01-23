package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;

/**
 * An exponential family factor which takes a FeatureExtractor at construction time and 
 * uses it to extract features.
 * 
 * @author mgormley
 */
public class FeExpFamFactor extends ExpFamFactor {

    private static final long serialVersionUID = 1L;
    private FeatureExtractor fe;
    
    public FeExpFamFactor(VarSet vars, Object templateKey, FeatureExtractor fe) {
        super(vars, templateKey);
        this.fe = fe;
    }

    @Override
    public FeatureVector getFeatures(int configId) {
        return fe.calcFeatureVector(this, configId);
    }
    
}
