package edu.jhu.gm;

import edu.jhu.prim.vector.IntDoubleSortedVector;

public class FeatureVector extends IntDoubleSortedVector {

    private static final long serialVersionUID = 1L;

    /**
     * Large FeatureVectors (e.g. 20,000+ features) should almost always be
     * constructed via a FeatureVectorBuilder, which has much faster support for
     * add() and set().
     */
    public FeatureVector() {
        super();
    }

    public FeatureVector(int initialSize) {
        super(initialSize);
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
