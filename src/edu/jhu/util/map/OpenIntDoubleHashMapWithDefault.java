package edu.jhu.hltcoe.util.map;

import cern.colt.map.OpenIntDoubleHashMap;

public class OpenIntDoubleHashMapWithDefault extends OpenIntDoubleHashMap {

    private double defaultValue;

    public OpenIntDoubleHashMapWithDefault(double defaultValue) {
        super();
        this.defaultValue = defaultValue;
    }

    public OpenIntDoubleHashMapWithDefault(double defaultValue, int initialCapacity) {
        super(initialCapacity);
        this.defaultValue = defaultValue;
    }

    public OpenIntDoubleHashMapWithDefault(double defaultValue, int initialCapacity,
            double minLoadFactor, double maxLoadFactor) {
        super(initialCapacity, minLoadFactor, maxLoadFactor);
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the value associated with the specified key. It is often a good
     * idea to first check with {@link #containsKey(int)} whether the given key
     * has a value associated or not, i.e. whether there exists an association
     * for the given key or not.
     * 
     * @param key
     *            the key to be searched for.
     * @return the value associated with the specified key; <tt>defaultValue</tt> if no
     *         such key is present.
     */
    public double get(int key, double defaultValue) {
        int i = indexOfKey(key);
        if (i < 0)
            return defaultValue; // not contained
        return values[i];
    }
    /**
     * Returns the value associated with the specified key.
     * It is often a good idea to first check with {@link #containsKey(int)} whether the given key has a value associated or not, i.e. whether there exists an association for the given key or not.
     *
     * @param key the key to be searched for.
     * @return the value associated with the specified key; <tt>defaultValue</tt> if no such key is present.
     */
    public double get(int key) {
        int i = indexOfKey(key);
        if (i<0) return defaultValue; //not contained
        return values[i];
    }
    
    public Object clone() {
        OpenIntDoubleHashMapWithDefault clone = (OpenIntDoubleHashMapWithDefault)super.clone();
        clone.defaultValue = this.defaultValue;
        return clone;
    }

}
