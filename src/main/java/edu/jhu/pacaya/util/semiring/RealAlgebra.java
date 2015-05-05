package edu.jhu.pacaya.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class RealAlgebra implements Semiring, Algebra {

    private static final long serialVersionUID = 1L;
    
    public static final RealAlgebra REAL_ALGEBRA = new RealAlgebra();

    @Override
    public double plus(double x, double y) {
        return x + y;
    }

    @Override
    public double times(double x, double y) {
        return x * y;
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
    public double zero() {
        return 0;
    }

    @Override
    public double one() {
        return 1;
    }

    @Override
    public double posInf() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double negInf() {
        return Double.NEGATIVE_INFINITY;
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

    @Override
    public double abs(double x) {
        return Math.abs(x);
    }

    @Override
    public double negate(double x) {
        return -x;
    }

    @Override
    public boolean gt(double x, double y) {
        return x > y;
    }

    @Override
    public boolean lt(double x, double y) {
        return x < y;
    }

    @Override
    public boolean gte(double x, double y) {
        return x >= y;
    }

    @Override
    public boolean lte(double x, double y) {
        return x <= y;
    }

    @Override
    public boolean eq(double a, double b, double delta) {
        if (a == b) {
            // This case is needed for infinity equality.
            return true;
        }
        return Math.abs(a - b) < delta;
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
    public boolean isNaN(double x) {
        return Double.isNaN(x);
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
    
    // Two Algebras / Semirings are equal if they are of the same class.
    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() == other.getClass()) { return true; }
        return false;
    }
    
}
