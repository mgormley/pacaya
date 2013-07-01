package edu.jhu.util.matrix;

import java.io.Serializable;

import edu.jhu.util.vector.IntIntEntry;

public interface IntegerMatrix extends Serializable {

    public int getNumRows();
    
    public int getNumColumns();

    public void decrement(int t, int k);

    public void increment(int t, int k);

    public int get(int t, int k);

    public void decrement(int t, int k, int decr);
    
    public void increment(int t, int k, int incr);

    public void set(IntegerMatrix n);
    
    public Iterable<IntIntEntry> getRowEntries(int row);
}