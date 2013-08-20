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

    private static final long serialVersionUID = 1L;
    int value;

    public MutableInt() {
        this.value = 1;  // Start at 1 since we're counting
    }
    
    public MutableInt(int value) {
        this.value = value;
    }
    
    public void increment() {
        ++value;
    }
    
    public void decrement() {
        --value;
    }

    public int get() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
