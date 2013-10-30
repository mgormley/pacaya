package edu.jhu.prim.matrix.infinite;

public interface InfiniteIntegerVector {

    int get(int k);

    void increment(int k);

    void decrement(int k);

    Iterable<Integer> getActiveIndices();

    void decrement(int k, int decr);
    
    void increment(int k, int incr);
    
}
