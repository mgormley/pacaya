package edu.jhu.util.semiring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.jhu.util.dist.Gaussian;

public class LogPosNegSemiringTest {

    private interface BinOp {
        double call(double x, double y, SemiringExt s);
    }

    private interface UnaryOp {
        double call(double x, SemiringExt s);
    }

    private static final int NUM_RANDS = 100;
    private LogPosNegSemiring sLog = new LogPosNegSemiring();
    private RealSemiring sReal = new RealSemiring();
            
    @Test
    public void testToFromReal() {
        toFromCheck(1);
        toFromCheck(-1);
        toFromCheck(Double.POSITIVE_INFINITY);
        toFromCheck(Double.NEGATIVE_INFINITY);
        toFromCheck(Double.NaN);

        int i = 0, j = 0;
        double nan3 = (double)i/j;
        toFromCheck(nan3);
    }
    
    private void toFromCheck(double x) {
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sLog.fromReal(x)));
        assertEquals(x, sLog.toReal(sLog.fromReal(x)), 1e-13);
    }

    @Test
    public void testTimes() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, SemiringExt s) {
                return s.times(x, y);
            }
        });
    }

    @Test
    public void testDivide() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, SemiringExt s) {
                return s.divide(x, y);
            }
        });
    }
    
    @Test
    public void testPlus() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, SemiringExt s) {
                return s.plus(x, y);
            }
        });
    }
    
    @Test
    public void testMinus() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, SemiringExt s) {
                return s.minus(x, y);
            }
        });
    }
    
    public void testBinaryOperation(BinOp lambda) {
        double x, y;
        x = 1;
        y = 1;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        
        // Test positive and negative numbers with abs(x) > 1. 
        x = 11.11;
        y = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = -11.11;
        y = 44.44;
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = 11.11;
        y = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = -11.11;
        y = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);         
        // Swap the relative scales of x and y and test again
        y = 11.11;
        x = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = -11.11;
        x = 44.44;
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = 11.11;
        x = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = -11.11;
        x = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 

        // Test numbers whose abs is equal.
        double abs = 13.1313;
        y = abs;
        x = -abs;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 
        y = abs;
        x = abs;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 
        y = -abs;
        x = abs;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 
        y = -abs;
        x = -abs;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 
        
        // Test positive and negative numbers with abs(x) < 1.
        x = 0.11;
        y = 0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = -0.11;
        y = 0.44;
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = 0.11;
        y = -0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        x = -0.11;
        y = -0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);         
        // Swap the relative scales of x and y and test again
        y = 0.11;
        x = 0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = -0.11;
        x = 0.44;
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = 0.11;
        x = -0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);
        y = -0.11;
        x = -0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13); 

        // Test with one value as an infinity.
        x = Double.POSITIVE_INFINITY;
        y = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);    
        x = Double.NEGATIVE_INFINITY;
        y = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);  
        x = Double.POSITIVE_INFINITY;
        y = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);    
        x = Double.NEGATIVE_INFINITY;
        y = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);  
        
        // Swap the position of the infinities and test again.
        y = Double.POSITIVE_INFINITY;
        x = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);    
        y = Double.NEGATIVE_INFINITY;
        x = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);  
        y = Double.POSITIVE_INFINITY;
        x = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);    
        y = Double.NEGATIVE_INFINITY;
        x = -44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-13);  
        
        for (int i=0; i<NUM_RANDS; i++) {
            x = Gaussian.nextDouble(0.0, 1000);
            y = Gaussian.nextDouble(0.0, 1000);                    
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), 1e-5);  
        }
    }
        
    private static double compute(BinOp lambda, double x, double y, SemiringExt s) {
        double compacted = lambda.call(s.fromReal(x), s.fromReal(y), s);
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(compacted));
        return s.toReal(compacted);
    }

    /**
     * The correct bit representation of -0 in this semiring happens to be a NaN.
     */
    @Test
    public void testAbsEqual() {   
        assertTrue(!Double.isNaN(sLog.fromReal(Double.POSITIVE_INFINITY)));
        assertTrue(Double.isNaN(sLog.fromReal(Double.NEGATIVE_INFINITY)));
        {
            double v1 = sLog.fromReal(3);
            double v2 = sLog.fromReal(-3);
            double sum = sLog.plus(v1, v2);
            System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sum));
            assertEquals(0l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY), Double.doubleToRawLongBits(sum));
            assertFalse(Double.isNaN(sum));
            assertFalse(Double.isNaN(sLog.toReal(sum)));
        }
        {
            double v1 = sLog.fromReal(-3);
            double v2 = sLog.fromReal(3);
            double sum = sLog.plus(v1, v2);
            System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sum));
            assertEquals(1l | Double.doubleToRawLongBits(Double.NEGATIVE_INFINITY), Double.doubleToRawLongBits(sum));
            assertTrue(Double.isNaN(sum));
            assertFalse(Double.isNaN(sLog.toReal(sum)));
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
    
    @Test
    public void testExp() {
        testUnaryOperation(false, new UnaryOp() {
            @Override
            public double call(double x, SemiringExt s) {
                return s.exp(x);
            }
        });
    }

    @Test
    public void testLog() {
        testUnaryOperation(true, new UnaryOp() {
            @Override
            public double call(double x, SemiringExt s) {
                return s.log(x);
            }
        });
    }
    
    public void testUnaryOperation(boolean failOnNegatives, UnaryOp lambda) {
        double x;
        x = 1;
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);
        x = 11.11;   
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);
        x = 0.11;   
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);
        x = Double.POSITIVE_INFINITY;
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);    
        
        if (!failOnNegatives) {
            x = -11.11;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);
            x = -0.11;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);
            x = Double.NEGATIVE_INFINITY;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-13);    
        } else {
            x = -11.11;
            try {
                compute(lambda, x, sLog);
                fail();
            } catch (Exception e) {
                // pass
            }
            x = -0.11;
            try {
                compute(lambda, x, sLog);
                fail();
            } catch (Exception e) {
                // pass         
            }
            x = Double.NEGATIVE_INFINITY;
            try {
                compute(lambda, x, sLog);
                fail();
            } catch (Exception e) {
                // pass         
            }
        }
        
        if (!failOnNegatives) {
            for (int i=0; i<NUM_RANDS; i++) {
                x = Gaussian.nextDouble(0.0, 1);                  
                assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-5);  
            }
        } else {
            for (int i=0; i<NUM_RANDS; i++) {
                x = Math.abs(Gaussian.nextDouble(0.0, 1));                  
                assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), 1e-5);  
            }
        }
    }
        
    private static double compute(UnaryOp lambda, double x, SemiringExt s) {
        double compacted = lambda.call(s.fromReal(x), s);
        System.out.printf("%0#16x %f\n", Double.doubleToRawLongBits(compacted), s.toReal(compacted));
        return s.toReal(compacted);
    }

}
