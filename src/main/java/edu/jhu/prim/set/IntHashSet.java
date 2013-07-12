package edu.jhu.prim.set;

import edu.jhu.prim.map.IntDoubleHashMap;

/**
 * Hash set for int primitives.
 * @author mgormley
 */
public class IntHashSet {

    private IntDoubleHashMap map = new IntDoubleHashMap();
    
    public void add(int key) {
        map.put(key, 1);
    }

    public boolean contains(int key) {
        return map.contains(key);
    }

}
