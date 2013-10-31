/**
 * 
 */
package edu.jhu.util.tuple;


public class UnorderedPair extends IntTuple {
    public UnorderedPair(int fac1, int fac2) {
        super(Math.min(fac1, fac2), Math.max(fac1, fac2));
    }

    public int get1() {
        return get(0);
    }

    public int get2() {
        return get(1);
    }
}