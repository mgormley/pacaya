package edu.jhu.prim.matrix;

import java.io.Serializable;

import edu.jhu.prim.map.IntDoubleEntry;

public interface DoubleMatrix extends Serializable {

    public int getNumRows();
    
    public int getNumColumns();

    public double get(int row, int col);
    
    public void set(int row, int col, double value);

    public void decrement(int row, int col, double decr);
    
    public void increment(int row, int col, double incr);

    public void set(DoubleMatrix other);
    
    public Iterable<IntDoubleEntry> getRowEntries(int row);
    
    public DoubleMatrix viewTranspose();
}