package edu.jhu.autodiff2;

import java.util.Arrays;

import edu.jhu.prim.arrays.IntArrays;

/**
 * An iterator over the indices of a multi-dimensional array.
 * @author mgormley
 */
public class DimIter {

    private int[] dims;
    private int numEntries;
    private int[] states;
    private int curIdx;
            
    public DimIter(int... dimensions) {
        this.dims = dimensions;
        this.numEntries = IntArrays.prod(dims);
        states = new int[dims.length];
        reset();
    }
    
    public void reset() {
        Arrays.fill(states, 0);
        int rightmost = states.length-1;
        states[rightmost] = -1;
        curIdx = -1;
    }

    public int[] next() {
        if (!hasNext()) {
            throw new IllegalStateException("No next state available");
        }
        int rightmost = states.length-1;
        states[rightmost]++;
        curIdx++;
        for (int i=rightmost; i >= 0; i--) {
            if (states[i] >= dims[i]) {
                states[i] = 0;
                if (i > 0) {
                    states[i-1]++;
                }
            }
        }
        return states;
    }
 
    public boolean hasNext() {
        return curIdx+1 < numEntries;
    }
}
