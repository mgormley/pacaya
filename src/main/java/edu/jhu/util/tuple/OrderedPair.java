/**
 * 
 */
package edu.jhu.util.tuple;

import edu.jhu.util.IntTuple;

public class OrderedPair extends IntTuple {
    public OrderedPair(int facIdx, int varIdx) {
        super(facIdx, varIdx);
    }

    public int get1() {
        return get(0);
    }

    public int get2() {
        return get(1);
    }
}