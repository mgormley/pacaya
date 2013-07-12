package edu.jhu.prim.map;

import java.util.Iterator;

public interface IntDoubleMap extends Iterable<IntDoubleEntry> {

    public abstract void clear();

    // TODO: rename to containsKey.
    public abstract boolean contains(int idx);

    public abstract double get(int idx);

    public abstract double getWithDefault(int idx, double defaultVal);

    public abstract void remove(int idx);

    public abstract void put(int idx, double val);

    public abstract Iterator<IntDoubleEntry> iterator();

    public abstract int size();

    /**
     * Returns the indices.
     */
    public abstract int[] getIndices();

    /**
     * Returns the values.
     */
    public abstract double[] getValues();

}