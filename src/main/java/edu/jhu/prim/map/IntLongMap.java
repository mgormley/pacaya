package edu.jhu.prim.map;

import java.util.Iterator;

public interface IntLongMap extends Iterable<IntLongEntry> {

    public abstract void clear();

    // TODO: rename to containsKey.
    public abstract boolean contains(int idx);

    public abstract long get(int idx);

    public abstract long getWithDefault(int idx, long defaultVal);

    public abstract void remove(int idx);

    public abstract void put(int idx, long val);

    public abstract Iterator<IntLongEntry> iterator();

    public abstract int size();

    /**
     * Returns the indices.
     */
    public abstract int[] getIndices();

    /**
     * Returns the values.
     */
    public abstract long[] getValues();

}