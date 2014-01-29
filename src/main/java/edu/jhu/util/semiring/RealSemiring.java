package edu.jhu.util.semiring;

public class RealSemiring implements Semiring {

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

}
