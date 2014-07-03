package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureVector;

public class ExplicitExpFamFactor extends ExpFamFactor {
    
    private static final long serialVersionUID = 1L;
    
    private FeatureVector[] features;

    public ExplicitExpFamFactor(VarSet vars) {
        super(vars);
        features = new FeatureVector[vars.calcNumConfigs()];
    }
    
    public ExplicitExpFamFactor(ExplicitFactor other) {
        super(other);
        features = new FeatureVector[other.getVars().calcNumConfigs()];
    }
    
    public ExplicitExpFamFactor(DenseFactor other) {
        super(other);
        features = new FeatureVector[other.getVars().calcNumConfigs()];
    }

    public void setFeatures(int config, FeatureVector features) {
        this.features[config] = features;
    }

    @Override
    public FeatureVector getFeatures(int config) {
        return this.features[config];
    }

}
