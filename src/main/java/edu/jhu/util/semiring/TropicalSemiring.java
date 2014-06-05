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

    // Two Algebras / Semirings are equal if they are of the same class.
    public boolean equal(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() == other.getClass()) { return true; }
        return false;
    }
    
}
