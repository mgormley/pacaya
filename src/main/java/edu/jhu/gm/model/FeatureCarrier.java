package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureVector;

public interface FeatureCarrier {

    FeatureVector getFeatures(int config);
    
}
