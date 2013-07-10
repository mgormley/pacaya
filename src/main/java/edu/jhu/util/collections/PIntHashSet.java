package edu.jhu.util.collections;


public class PIntHashSet {

    private PIntDoubleHashMap map = new PIntDoubleHashMap();
    
    public void add(int key) {
        map.put(key, 1);
    }

    public boolean contains(int key) {
        return map.contains(key);
    }

}
