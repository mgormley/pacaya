package edu.jhu.gm;

import edu.jhu.util.vector.SortedIntDoubleVector;

public class FeatureVector extends SortedIntDoubleVector {

    /**
     * Feature Vectors should almost always be constructed via a
     * FeatureVectorBuilder, which has much faster support for add() and set().
     */
    public FeatureVector() {
        super();
    }
    
    /** Copy constructor. */
    public FeatureVector(FeatureVector featureVector) {
        super(featureVector);
    }

    /** Constructs a feature vector from a dense vector represented as an array. */
    public FeatureVector(double[] feats) {
        super(feats);
    }

    public FeatureVector(int[] index, double[] values) {
        super(index, values);
    }

}
