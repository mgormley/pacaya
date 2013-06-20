package edu.jhu.util.collections;

import cern.colt.map.tlong.OpenLongIntHashMap;

public class PLongIntHashMap extends OpenLongIntHashMap {

    public boolean contains(long key) {
        return this.containsKey(key);
    }

}
