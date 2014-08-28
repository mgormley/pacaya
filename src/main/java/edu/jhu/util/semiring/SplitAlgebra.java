package edu.jhu.util.semiring;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import edu.jhu.prim.Primitives;

/**
 * Algebra for testing the use of algebras. The internal 64-bit representation of a number is given
 * by a concatenation of two 32-bit representations of numbers in other algebras. These other
 * algebras are carefully chosen so that casting from their internal double (64-bit) representation
 * to a float (32-bit) representation is safe. If the cast isn't safe, an exception is thrown.
 * 
 * This algebra is intended to be used for testing since it can detect whenever the internal
 * representations of the two numbers diverge. 
 * 
 * @author mgormley
 */
public class SplitAlgebra extends AbstractToFromRealAlgebra implements Algebra {

    private static final Logger log = Logger.getLogger(SplitAlgebra.class);

    private static final int SHIFTER = 32;
    private static final long MASK_1 = 0xFFFFFFFFl;
    private static final long MASK_2 = ~MASK_1;
    
    private Algebra a1;
    private Algebra a2;
    private String format;
    private double delta;
    
    /** Constructs an algebra which will detect whether two separately stored algebras are correctly tracking each other. */
    public SplitAlgebra() {
        this.a1 = new RealAlgebra();
        this.a2 = new ShiftedRealAlgebra();
        this.format = "%12.6e";
        // Machine epsilon for a 32-bit number is 1.19e-07.
        this.delta = 1.19e-06;
    }
    
    @Override
    public double toReal(double nonReal) {
        long xd = Double.doubleToRawLongBits(nonReal);
        long l1 = xd & MASK_1;
        long l2 = (xd & MASK_2) >>> SHIFTER;
        float f1 = Float.intBitsToFloat((int) l1);
        float f2 = Float.intBitsToFloat((int) l2);
        double real1 = a1.toReal((double) f1);
        double real2 = a2.toReal((double) f2);
        if (!safeMantissaDoubleEquals(real1, real2)) {
            String msg = String.format("Values have diverged within the algebra representation:\n");
            msg += String.format("v1=%f v2=%f\n", real1, real2);
            msg += String.format("f1=%f f2=%f\n", f1, f2);
            msg += String.format("l1=%x l2=%x\n", l1, l2);
            msg += String.format("xd=%x\n", xd);
            msg += String.format("rstr1="+format+" rstr2="+format+"\n", real1, real2);
            throw new RuntimeException(msg);
        }
        return real1;
    }

    private boolean safeMantissaDoubleEquals(double real1, double real2) {
        return Primitives.equals(real1, real2, delta) || 
                String.format(format, real1).equals(String.format(format, real2));
    }

    @Override
    public double fromReal(double real) {
        // We assume that casting the given algebra from double to float leaves the value intact.
        // This is not the case for certain algebras such as the LogSignAlgebra (or this
        // SplitAlgebra) which internally do bit operations.
        float f1, f2;
        if (Double.isNaN(real)) {
            f1 = Float.NaN;
            f2 = Float.NaN;
        } else {
            checkThatFloatCastIsSafe(a1, real);
            checkThatFloatCastIsSafe(a2, real);
            f1 = (float) a1.fromReal(real);
            f2 = (float) a2.fromReal(real);
        }
        long l1 = intBitsToLong(Float.floatToRawIntBits(f1));
        long l2 = intBitsToLong(Float.floatToRawIntBits(f2));
        double xd = Double.longBitsToDouble((l2 << SHIFTER) | l1);
        return xd;
    }

    private long intBitsToLong(int intBits) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putInt(0);
        bb.putInt(intBits);        
        return bb.getLong(0);
    }

    private void checkThatFloatCastIsSafe(Algebra a, double real) {
        double xdPreCast = a.fromReal(real);
        float f = (float) xdPreCast;
        double xdPostCast = (double) f;
        double realPostCast = a.toReal(xdPostCast);
        if (!safeMantissaDoubleEquals(real, realPostCast)) {
            String msg = String.format("Unsafe to cast to float: a=" + a + "\n");
            msg += String.format("realPreCast=%f realPostCast=%f\n", real, realPostCast);
            msg += String.format("rstr1="+format+" rstr2="+format+"\n", real, realPostCast);
            throw new RuntimeException(msg);
        }
    }

}
