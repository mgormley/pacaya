package edu.jhu.pacaya.gm.model;

import edu.jhu.pacaya.gm.feat.FeatureVector;

public interface FeatureCarrier {

    FeatureVector getFeatures(int config);
    
}
