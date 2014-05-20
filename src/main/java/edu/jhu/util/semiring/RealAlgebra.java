package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class RealAlgebra implements Semiring, Algebra {

    @Override
    public double plus(double x, double y) {
        return x + y;
    }

    @Override
    public double times(double x, double y) {
        return x * y;
    }

    @Override
    public double zero() {
        return 0;
    }

    @Override
    public double one() {
        return 1;
    }

    @Override
    public double minus(double x, double y) {
        return x - y;
    }
    
    @Override
    public double divide(double x, double y) {
        return x / y;
    }

    @Override
    public double toReal(double nonReal) {
        return nonReal;
    }

    @Override
    public double fromReal(double real) {
        return real;
    }

    @Override
    public double toLogProb(double nonReal) {
        return FastMath.log(nonReal);
    }

    @Override
    public double fromLogProb(double logProb) {
        return FastMath.exp(logProb);
    }
    
    @Override
    public double exp(double x) {
        return FastMath.exp(x);
    }

    @Override
    public double log(double x) {
        if (x < 0) {
            throw new IllegalStateException("Unable to take the log of a negative number.");
        }
        return FastMath.log(x);
    }
    
}
