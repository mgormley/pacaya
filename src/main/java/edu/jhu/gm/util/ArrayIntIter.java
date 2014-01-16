package edu.jhu.gm.util;

public class ArrayIntIter implements IntIter {

    private final int[] values;
    private int cur;
        
    /** 
     * Constructs a new IntIter which ranges from start (inclusive) to end (exclusive) by steps of incr.
     * 
     * @param values The values over which to iterate.
     * @param incr The increment value.
     */
    public ArrayIntIter(int... values) {
        this.values = values;
        this.cur = 0;
    }
    
    @Override
    public int next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        return values[cur++];
    }

    @Override
    public boolean hasNext() {
        return cur < values.length;
    }

    @Override
    public void reset() {
        this.cur = 0;
    }
    
}
