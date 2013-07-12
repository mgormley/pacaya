package edu.jhu.gm;

import edu.jhu.prim.map.IntDoubleHashMap;
import edu.jhu.util.Pair;
import edu.jhu.util.Sort;

/**
 * For building large (e.g. 20000+) sparse feature vectors quickly.
 * @author mgormley
 */
public class FeatureVectorBuilder extends IntDoubleHashMap {

    public FeatureVectorBuilder() {
        super(0.0);
    }
    
    public void add(int index, double value) {
        this.put(index, this.get(index) + value);
    }
    
    public FeatureVector toFeatureVector() {
        Pair<int[], double[]> ivs = this.getIndicesAndValues();
        int[] index = ivs.get1();
        double[] values = ivs.get2();
        Sort.sortIndexAsc(index, values);
        return new FeatureVector(index, values);
    }
    
}
