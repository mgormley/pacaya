package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class RealSemiring implements Semiring, SemiringExt {

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
    
}
