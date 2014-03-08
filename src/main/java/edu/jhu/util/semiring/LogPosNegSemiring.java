package edu.jhu.util.semiring;

import edu.jhu.prim.util.math.FastMath;

public class LogPosNegSemiring implements Semiring, SemiringExt {

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
    public static final double negate(double xd) {
        return Double.longBitsToDouble(SIGN_MASK ^ Double.doubleToRawLongBits(xd));
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
    public double zero() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double one() {
        return 0;
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
    
}
