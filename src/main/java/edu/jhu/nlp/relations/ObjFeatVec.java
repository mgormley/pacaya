package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.util.Lambda;

public class ObjFeatVec<T> {

    private List<T> keys;
    private DoubleArrayList vals;
    
    public ObjFeatVec() {
        keys = new ArrayList<>();
        vals = new DoubleArrayList();
    }
    
    public void add(T key, double val) {
        keys.add(key);
        vals.add(val);
    }
    
    public T getKey(int i) {
        return keys.get(i);
    }
    
    public double getVal(int i) {
        return vals.get(i);
    }
    
    public void iterate(Lambda.FnObjDoubleToVoid<T> lambda) {
        for (int i=0; i<keys.size(); i++) {
            lambda.call(keys.get(i), vals.get(i));
        }
    }
    
}
