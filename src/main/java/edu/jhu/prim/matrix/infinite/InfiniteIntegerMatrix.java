package edu.jhu.prim.matrix.infinite;

public interface InfiniteIntegerMatrix {

	public int getNumRows();

    public void decrement(int row, int col);

    public void increment(int row, int col);

    public int get(int row, int col);

    public void decrement(int row, int col, int decr);
    
    public void increment(int row, int col, int incr);
    
}