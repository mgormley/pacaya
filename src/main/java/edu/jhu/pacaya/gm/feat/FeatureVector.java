package edu.jhu.pacaya.gm.feat;

import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.prim.util.Lambda.FnIntDoubleToVoid;
import edu.jhu.prim.vector.IntDoubleUnsortedVector;
import edu.jhu.prim.vector.IntDoubleVector;

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
        super(featureVector);
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
    
    // TODO: Move this to prim.
    public int getMaxIdx() {
        int max = Integer.MIN_VALUE;
        for (int i=0; i<this.top; i++) {
            if (idx[i] > max) {
                max = idx[i];
            }
        }
        return max;
    }
    
    @Override
    public void apply(FnIntDoubleToDouble function) {
        // Feature vectors never call: compact();
        for(int i=0; i<top; i++) {
            vals[i] = function.call(idx[i], vals[i]);
        }
    }

    @Override
    public void iterate(FnIntDoubleToVoid function) {
        // Feature vectors never call: compact();
        for(int i=0; i<top; i++) {
            function.call(idx[i], vals[i]);
        }
    }

}
