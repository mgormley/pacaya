package edu.jhu.util.collections;

import cern.colt.map.tlong.OpenLongIntHashMap;

public class PLongHashSet {

    private OpenLongIntHashMap map = new OpenLongIntHashMap();

    public void add(long key) {
        map.put(key, 1);
    }

    public boolean contains(long key) {
        return map.containsKey(key);
    }

}
