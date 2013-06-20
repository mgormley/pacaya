package edu.jhu.util.collections;

import cern.colt.map.OpenIntIntHashMap;

public class PIntHashSet {

    private OpenIntIntHashMap map = new OpenIntIntHashMap();
    
    public void add(int key) {
        map.put(key, 1);
    }

    public boolean contains(int key) {
        return map.containsKey(key);
    }

}
