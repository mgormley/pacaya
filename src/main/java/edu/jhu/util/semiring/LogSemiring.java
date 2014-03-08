package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class LogSemiring implements Semiring, SemiringExt {

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
    public double minus(double x, double y) {
        return FastMath.logSubtract(x, y);
    }
    
    @Override
    public double divide(double x, double y) {
        return x - y;
    }
    
}
