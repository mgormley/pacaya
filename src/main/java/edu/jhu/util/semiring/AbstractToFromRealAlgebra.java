package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

/**
 * An abstract class for abstract algebras which ONLY define conversion to / from the reals. All
 * other algebraic operations are obtained by converting to the reals, carrying out the operation
 * and converting back to the algebraic representation.
 * 
 * @author mgormley
 */
public abstract class AbstractToFromRealAlgebra implements Semiring, Algebra {

    @Override
    public abstract double toReal(double nonReal);

    @Override
    public abstract double fromReal(double real);

    @Override
    public double plus(double x, double y) {
        return fromReal(toReal(x) + toReal(y));
    }

    @Override
    public double times(double x, double y) {
        return fromReal(toReal(x) * toReal(y));
    }

    @Override
    public double minus(double x, double y) {
        return fromReal(toReal(x) - toReal(y));
    }
    
    @Override
    public double divide(double x, double y) {
        return fromReal(toReal(x) / toReal(y));
    }

    @Override
    public double zero() {
        return fromReal(0);
    }

    @Override
    public double one() {
        return fromReal(1);
    }

    @Override
    public double posInf() {
        return fromReal(Double.POSITIVE_INFINITY);
    }

    @Override
    public double negInf() {
        return fromReal(Double.NEGATIVE_INFINITY);
    }
    
    @Override
    public double exp(double x) {
        return fromReal(FastMath.exp(toReal(x)));
    }

    @Override
    public double log(double x) {
        if (toReal(x) < 0) {
            throw new IllegalStateException("Unable to take the log of a negative number.");
        }
        return fromReal(FastMath.log(toReal(x)));
    }

    @Override
    public double abs(double x) {
        return fromReal(Math.abs(toReal(x)));
    }

    @Override
    public double negate(double x) {
        return fromReal(-toReal(x));
    }

    @Override
    public boolean gt(double x, double y) {
        return toReal(x) > toReal(y);
    }

    @Override
    public boolean lt(double x, double y) {
        return toReal(x) < toReal(y);
    }

    @Override
    public boolean gte(double x, double y) {
        return toReal(x) >= toReal(y);
    }

    @Override
    public boolean lte(double x, double y) {
        return toReal(x) <= toReal(y);
    }

    @Override
    public boolean eq(double a, double b, double delta) {
        if (toReal(a) == toReal(b)) {
            // This case is needed for infinity equality.
            return true;
        }
        return Math.abs(toReal(a) - toReal(b)) < delta;
    }
    
    @Override
    public double toLogProb(double nonReal) {
        return fromReal(FastMath.log(toReal(nonReal)));
    }

    @Override
    public double fromLogProb(double logProb) {
        return fromReal(FastMath.exp(toReal(logProb)));
    }

    @Override
    public boolean isNaN(double x) {
        return Double.isNaN(toReal(x));
    }

    @Override
    public double minValue() {
        return fromReal(Double.NEGATIVE_INFINITY);
    }

    @Override
    public String toString() {
        return this.getClass().toString();
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
