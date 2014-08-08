package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class LogSignAlgebra implements Semiring, Algebra {

    // We choose the least significant digit of the mantissa as our sign bit.
    // This bit is chosen for two reasons: (1) the various bit representations
    // of NaN, +inf, and -inf seem to use only the most significant bits of the 
    // mantissa or none of the mantissa at all and (2) if there's a bug in our code
    // in which we forget to convert from this semiring back to the reals, it should
    // only result in a sign error which will (hopefully) be easier to find.
    private static final int SIGN_BIT = 1;
    private static final long SIGN_MASK = 0x1 << (SIGN_BIT-1);
    private static final long FLOAT_MASK = ~SIGN_MASK;
    
    private static final long POSITIVE = 0;
    private static final long NEGATIVE = 1;
    
    /** Converts a compacted number to its real value. */
    @Override
    public double toReal(double x) {
        double unsignedReal = FastMath.exp(natlog(x));
        return (sign(x) == POSITIVE) ? unsignedReal : -unsignedReal;
    }
    
    /** Converts a real value to its compacted representation. */
    @Override 
    public double fromReal(double x) {
        long sign = POSITIVE;
        if (x < 0) {
            sign = NEGATIVE;
            x = -x;  
        }
        return compact(sign, FastMath.log(x));
    }

    @Override
    public double toLogProb(double nonReal) {
        if (sign(nonReal) == NEGATIVE) {
            throw new IllegalStateException("Unable to take the log of a negative number.");
        }
        return natlog(nonReal);
    }
    
    @Override
    public double fromLogProb(double logProb) {
        return compact(POSITIVE, natlog(logProb));
    }
    
    /** Gets the sign bit of the compacted number. */
    public static final long sign(double xd) {
        return SIGN_MASK & Double.doubleToRawLongBits(xd);
    }
    
    /** Gets the natural log portion of the compacted number. */
    public static final double natlog(double xd) {
        return Double.longBitsToDouble(FLOAT_MASK & Double.doubleToRawLongBits(xd));
    }
    
    /** Gets the compacted version from the sign and natural log. */
    public static final double compact(long sign, double natlog) {
        return Double.longBitsToDouble(sign | (FLOAT_MASK & Double.doubleToRawLongBits(natlog)));
    }
    
    /** Negates the compacted number. */
    public double negate(double xd) {
        return Double.longBitsToDouble(SIGN_MASK ^ Double.doubleToRawLongBits(xd));
    }

    @Override
    public double abs(double xd) {
        return compact(POSITIVE, natlog(xd));
    }
        
    @Override
    public double plus(double x, double y) {
        long sx = sign(x);
        long sy = sign(y);
        double lx = natlog(x);
        double ly = natlog(y);
        if (sx == POSITIVE && sy == POSITIVE) {
            return compact(POSITIVE, FastMath.logAdd(lx, ly));
        } else if (sx == POSITIVE && sy == NEGATIVE) {
            double diff = FastMath.logSubtract(Math.max(lx, ly), Math.min(lx, ly));
            long sign = (lx >= ly) ? POSITIVE : NEGATIVE;
            return compact(sign, diff);
        } else if (sx == NEGATIVE && sy == POSITIVE) {
            double diff = FastMath.logSubtract(Math.max(lx, ly), Math.min(lx, ly));
            long sign = (lx >= ly) ? NEGATIVE : POSITIVE;
            return compact(sign, diff);
        } else {
            return compact(NEGATIVE, FastMath.logAdd(lx, ly));
        }
    }

    @Override
    public double times(double x, double y) {
        long sign = (sign(x) == sign(y)) ? POSITIVE : NEGATIVE;
        return compact(sign, natlog(x) + natlog(y));
    }

    @Override
    public double minus(double x, double y) {
        return plus(x, negate(y));
    }
    
    @Override
    public double divide(double x, double y) {
        long sign = (sign(x) == sign(y)) ? POSITIVE : NEGATIVE;
        return compact(sign, natlog(x) - natlog(y));
    }
    
    public double exp(double x) {
        return compact(POSITIVE, toReal(x));
    }

    public double log(double x) {
        if (sign(x) == NEGATIVE) {
            throw new IllegalStateException("Unable to take the log of a negative number.");
        }
        return fromReal(natlog(x));
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
    public double posInf() {
        return fromReal(Double.POSITIVE_INFINITY);
    }

    @Override
    public double negInf() {
        return fromReal(Double.NEGATIVE_INFINITY);
    }

    @Override
    public double minValue() {
        return fromReal(Double.NEGATIVE_INFINITY);
    }

    @Override
    public boolean gt(double x, double y) {
        long sx = sign(x);
        long sy = sign(y);
        double lx = natlog(x);
        double ly = natlog(y);
        if (sx == POSITIVE && sy == POSITIVE) {
            return lx > ly;
        } else if (sx == POSITIVE && sy == NEGATIVE) {
            return true;
        } else if (sx == NEGATIVE && sy == POSITIVE) {
            return false;
        } else {
            return lx < ly;
        }
    }

    @Override
    public boolean lt(double x, double y) {
        return gt(y, x);
    }

    @Override
    public boolean gte(double x, double y) {
        long sx = sign(x);
        long sy = sign(y);
        double lx = natlog(x);
        double ly = natlog(y);
        if (sx == POSITIVE && sy == POSITIVE) {
            return lx >= ly;
        } else if (sx == POSITIVE && sy == NEGATIVE) {
            return true;
        } else if (sx == NEGATIVE && sy == POSITIVE) {
            return false;
        } else {
            return lx <= ly;
        }
    }

    @Override
    public boolean lte(double x, double y) {
        return gte(y, x);
    }

    @Override
    public boolean eq(double x, double y, double delta) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public boolean isNaN(double x) {
        // TODO: This requires testing.
        return Double.isNaN(natlog(x));
    }
    
    // Two Algebras / Semirings are equal if they are of the same class.
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() == other.getClass()) { return true; }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
    
}
