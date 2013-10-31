/**
 * 
 */
package edu.jhu.util.tuple;

import java.util.Arrays;

public class DoubleTuple {
    
    private final double[] x;
    
    public DoubleTuple(double... args) {
        x = new double[args.length];
        for (int i=0; i<args.length; i++) {
            x[i] = args[i];
        }
    }
    
    public int size() {
        return x.length;
    }
    
    public double get(int i) {
        return x[i];
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(x);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DoubleTuple other = (DoubleTuple) obj;
        if (!Arrays.equals(x, other.x))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(x);
    }
    
}