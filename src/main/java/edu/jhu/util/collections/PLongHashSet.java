package edu.jhu.util.collections;

/**
 * Hash set for long primitives.
 * @author mgormley
 */
public class PLongHashSet {

    private PLongDoubleHashMap map = new PLongDoubleHashMap();

    public void add(long key) {
        map.put(key, 1);
    }

    public boolean contains(long key) {
        return map.contains(key);
    }

}
