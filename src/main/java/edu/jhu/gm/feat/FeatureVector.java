package edu.jhu.gm.feat;

import edu.jhu.prim.vector.IntDoubleUnsortedVector;

public class FeatureVector extends IntDoubleUnsortedVector {

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
        super(featureVector.capacity());
        // cast ensures it is dispatched to the correct implementation
        this.add((travis.Vector) featureVector); 
    }

    public FeatureVector(int[] index, double[] values) {
        super(index, values);
    }

    /** Constructs a feature vector from a dense vector represented as an array. */
    public FeatureVector(double[] feats) {
        super(getIndicesUpTo(feats.length), feats);
    }
    
    private static int[] getIndicesUpTo(int n) {
        int[] idx = new int[n];
        for(int i=0; i<n; i++)
            idx[i] = i;
        return idx;
    }

}
