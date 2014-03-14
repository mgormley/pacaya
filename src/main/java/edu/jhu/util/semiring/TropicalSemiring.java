package edu.jhu.util.semiring;

public class TropicalSemiring implements Semiring {

    @Override
    public double plus(double x, double y) {
        return Math.min(x, y);
    }

    @Override
    public double times(double x, double y) {
        return x + y;
    }

    @Override
    public double zero() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double one() {
        return 0;
    }

}
