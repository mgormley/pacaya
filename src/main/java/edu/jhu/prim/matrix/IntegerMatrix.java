package edu.jhu.prim.matrix;

import java.io.Serializable;

import edu.jhu.prim.map.IntIntEntry;

public interface IntegerMatrix extends Serializable {

    public int getNumRows();
    
    public int getNumColumns();

    public void decrement(int row, int col);

    public void increment(int row, int col);

    public int get(int row, int col);

    public void set(int row, int col, int value);

    public void decrement(int row, int col, int decr);
    
    public void increment(int row, int col, int incr);

    public void set(IntegerMatrix other);
    
    public Iterable<IntIntEntry> getRowEntries(int row);
}