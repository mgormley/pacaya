package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class LogSemiring implements Algebra {

    @Override
    public double plus(double x, double y) {
        return FastMath.logAdd(x, y);
    }

    @Override
    public double times(double x, double y) {
        return x + y;
    }

    @Override
    public double zero() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double one() {
        return 0;
    }

    @Override
    public double toReal(double nonReal) {
        return FastMath.exp(nonReal);
    }

    @Override
    public double fromReal(double real) {
        if (real < 0) {
            throw new IllegalStateException("LogSemiring only supports positive reals: " + real);
        }
        return FastMath.log(real);
    }

    @Override
    public double toLogProb(double nonReal) {
        return nonReal;
    }

    @Override
    public double fromLogProb(double logProb) {
        return logProb;
    }
    
    @Override
    public double minus(double x, double y) {
        return FastMath.logSubtract(x, y);
    }
    
    @Override
    public double divide(double x, double y) {
        return x - y;
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
