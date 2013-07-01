package edu.jhu.util.matrix.infinite;

public interface InfiniteIntegerMatrix {

	public int getNumRows();

    public void decrement(int t, int k);

    public void increment(int t, int k);

    public int get(int t, int k);

    public void decrement(int t, int k, int decr);
    
    public void increment(int t, int k, int incr);
    
}