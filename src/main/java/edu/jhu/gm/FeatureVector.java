package edu.jhu.gm;

import edu.jhu.util.vector.SortedIntDoubleVector;

public class FeatureVector extends SortedIntDoubleVector {

    public FeatureVector() {
        super();
    }
    
    public FeatureVector(FeatureVector featureVector) {
        super(featureVector);
    }

    public FeatureVector(double[] feats) {
        super(feats);
    }

}
