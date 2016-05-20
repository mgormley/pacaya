package edu.jhu.pacaya.util.semiring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LogSignAlgebraTest extends AbstractAlgebraTest {
    
    @Override
    public Algebra getAlgebra() {
        return LogSignAlgebra.getInstance();
    }
    
    /**
     * The correct bit representation of -0 in this semiring happens to be a NaN.
     */
    @Test
    public void testAbsEqual() {   
        assertTrue(!Double.isNaN(sThis.fromReal(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(sThis.fromReal(Double.NEGATIVE_INFINITY)));
        {
            double v1 = sThis.fromReal(3);
            double v2 = sThis.fromReal(-3);
            double sum = sThis.plus(v1, v2);
            System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sum));
            assertEquals(0l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY), Double.doubleToRawLongBits(sum));
            assertFalse(Double.isNaN(sum));
            assertFalse(Double.isNaN(sThis.toReal(sum)));
        }
        {
            double v1 = sThis.fromReal(-3);
            double v2 = sThis.fromReal(3);
            double sum = sThis.plus(v1, v2);
            System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sum));
            assertEquals(1l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY), Double.doubleToRawLongBits(sum));
            assertTrue(Double.isNaN(sum));
            assertFalse(Double.isNaN(sThis.toReal(sum)));
        }
    }
    
    @Test
    public void countNaNs() {
        // This shows that the internal representation of some numbers will CORRECTLY be NaN.
        long numNaNs = 0;
        if (Double.isNaN(Double.longBitsToDouble(1l | Double.doubleToRawLongBits(Double.POSITIVE_INFINITY)))) { numNaNs++; }
        if (Double.isNaN(Double.longBitsToDouble(1l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY)))) { numNaNs++; }
        if (Double.isNaN(Double.longBitsToDouble(0l | Double.doubleToRawLongBits(Double.POSITIVE_INFINITY)))) { numNaNs++; }
        if (Double.isNaN(Double.longBitsToDouble(0l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY)))) { numNaNs++; }
        System.out.println("Number of longs which are NaNs: " + numNaNs);
    }
    
    @Test
    public void testSomeFloatingPointRepresentations() {
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(1));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(10));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(100));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(1.00001));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(Double.NaN));
        
        int i = 0, j = 0;
        double nan1 = (double)0/0;
        double nan2 = (double)0/0;
        double nan3 = (double)i/j;
        System.out.printf("nan1 0x%x\n", Double.doubleToRawLongBits(nan1));
        System.out.printf("nan2 0x%x\n", Double.doubleToRawLongBits(nan2));
        System.out.printf("nan3 0x%x\n", Double.doubleToRawLongBits(nan3));

        System.out.printf("0x%x\n", Double.doubleToRawLongBits(Double.POSITIVE_INFINITY));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY));

        //fail("Not yet implemented");
    }

}
