package edu.jhu.srl;

import java.io.Serializable;

/**
 * A mutable integer for use in hash maps where the value is a count, to be
 * incremented.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class MutableInt implements Serializable {

    int value = 1; // Start at 1 since we're counting

    public void increment() {
        ++value;
    }

    public int get() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
