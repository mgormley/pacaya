package edu.jhu.prim.set;

import edu.jhu.prim.map.LongDoubleHashMap;

/**
 * Hash set for long primitives.
 * @author mgormley
 */
public class LongHashSet {

    private LongDoubleHashMap map = new LongDoubleHashMap();

    public void add(long key) {
        map.put(key, 1);
    }

    public boolean contains(long key) {
        return map.contains(key);
    }

}
