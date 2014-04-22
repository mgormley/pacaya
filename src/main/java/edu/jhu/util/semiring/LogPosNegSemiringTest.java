package edu.jhu.util.semiring;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.prim.util.math.FastMath;
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
    public void travisTest() {
        // code:
//        double oldBeta = beta[i];
//        double oldBetaFoe = betaFoe[i];
//        beta[i] = s.plus(beta[i], prod);
//        betaFoe[i] = s.plus(betaFoe[i], prodFoe);
        
        // results
//        370461   DEBUG Hyperalgo - oldBeta: -4848124998864338944
//        370461   DEBUG Hyperalgo - oldBetaFoe: -4593159590437072925
//                 DEBUG Hyperalgo - prod: 4375247037990436864
//        370461   DEBUG Hyperalgo - prodFoe: -4593159590437072926
//        370461   DEBUG Hyperalgo - beta: 4604418534313441774
//        370461   DEBUG Hyperalgo - betaFoe: 9221120237041090560
//        370461   DEBUG Hyperalgo - beta is NaN: false
//        370461   DEBUG Hyperalgo - betaFoe is NaN: true
//        370461   DEBUG Hyperalgo - s.class: class edu.jhu.util.semiring.LogPosNegSemiring
//        370461   DEBUG Hyperalgo - beta == betaFoe: false

        double betaFoe = Double.longBitsToDouble(-4593159590437072925l);
        double prodFoe = Double.longBitsToDouble(-4593159590437072926l);
        LogPosNegSemiring s = new LogPosNegSemiring();
        assertFalse(Double.isNaN(s.plus(betaFoe, prodFoe)));
    }
    
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
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(nan1));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(nan2));
        System.out.printf("0x%x\n", Double.doubleToRawLongBits(nan3));

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
