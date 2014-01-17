package edu.jhu.gm.util;

public class IncrIntIter implements IntIter {

    private final int start;
    private final int end;
    private final int incr;   
    private int cur;
        
    /** 
     * Constructs a new IntIter which ranges from start (inclusive) to end (exclusive) by steps of incr.
     * 
     * @param start The first value (inclusive) over which to iterate.
     * @param end The last value (exclusive) over which to iterate.
     * @param incr The increment value.
     */
    public IncrIntIter(int start, int end, int incr) {
        this.start = start;
        this.end = end;
        this.incr = incr;
        this.cur = start;
    }

    /** Standard constructor which increments by 1 from 0 to end. */
    public IncrIntIter(int end) {
        this(0, end, 1);
    }
    
    @Override
    public int next() {
        if (!hasNext()) {
            throw new IllegalStateException();
        }
        int tmp = cur;
        cur += incr;
        return tmp;
    }

    @Override
    public boolean hasNext() {
        return cur < end;
    }

    @Override
    public void reset() {
        this.cur = start;
    }
    
}
