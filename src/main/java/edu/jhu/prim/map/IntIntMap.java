package edu.jhu.prim.map;

import java.util.Iterator;

public interface IntIntMap extends Iterable<IntIntEntry> {

    public abstract void clear();

    // TODO: rename to containsKey.
    public abstract boolean contains(int idx);

    public abstract int get(int idx);

    public abstract int getWithDefault(int idx, int defaultVal);

    public abstract void remove(int idx);

    public abstract void put(int idx, int val);

    public abstract Iterator<IntIntEntry> iterator();

    public abstract int size();

    /**
     * Returns the indices.
     */
    public abstract int[] getIndices();

    /**
     * Returns the values.
     */
    public abstract int[] getValues();

}