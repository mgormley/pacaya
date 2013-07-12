package edu.jhu.util.collections;

/**
 * Hash set for int primitives.
 * @author mgormley
 */
public class PIntHashSet {

    private PIntDoubleHashMap map = new PIntDoubleHashMap();
    
    public void add(int key) {
        map.put(key, 1);
    }

    public boolean contains(int key) {
        return map.contains(key);
    }

}
