package edu.jhu.util.semiring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.jhu.util.dist.Gaussian;

public abstract class AbstractAlgebraTest {

    protected interface BinOp {
        double call(double x, double y, Algebra s);
    }

    protected interface UnaryOp {
        double call(double x, Algebra s);
    }

    protected static final int NUM_RANDS = 100;
    protected Algebra sLog = getAlgebra();
    protected RealAlgebra sReal = new RealAlgebra();
    protected double deltaStrict = 1e-13;
    protected double deltaLoose = 1e-5;
            
    public abstract Algebra getAlgebra();

    /** Override this method if the support of the Algebra is only the non-negative numbers. */
    public boolean supportsNegatives() {
        return true;
    }
    
    @Test
    public void testEquals() {
        Algebra s1 = getAlgebra();
        Algebra s2 = getAlgebra();
        assertTrue(s1.equals(s2));
    }
    
    @Test
    public void testToFromReal() {
        toFromCheck(1);
        toFromCheck(0);
        toFromCheck(Double.POSITIVE_INFINITY);
        toFromCheck(Double.NaN);
        if (supportsNegatives()) {
            toFromCheck(-1);
            toFromCheck(Double.NEGATIVE_INFINITY);
        }

        int i = 0, j = 0;
        double nan3 = (double)i/j;
        toFromCheck(nan3);
    }
    
    protected void toFromCheck(double x) {
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sLog.fromReal(x)));
        assertEquals(x, sLog.toReal(sLog.fromReal(x)), deltaStrict);
    }
    
    @Test
    public void testToFromLogProb() {
        toFromLogProbCheck(0);
        toFromLogProbCheck(-1);
        toFromLogProbCheck(Double.NEGATIVE_INFINITY);
        toFromLogProbCheck(Double.NaN);

        int i = 0, j = 0;
        double nan3 = (double)i/j;
        toFromLogProbCheck(nan3);
    }
    
    protected void toFromLogProbCheck(double x) {
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(sLog.fromLogProb(x)));
        assertEquals(x, sLog.toLogProb(sLog.fromLogProb(x)), deltaStrict);
    }

    @Test
    public void testTimes() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, Algebra s) {
                return s.times(x, y);
            }
        });
    }

    @Test
    public void testDivide() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, Algebra s) {
                return s.divide(x, y);
            }
        });
    }
    
    @Test
    public void testPlus() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, Algebra s) {
                return s.plus(x, y);
            }
        });
    }
    
    @Test
    public void testMinus() {
        testBinaryOperation(new BinOp() {            
            @Override
            public double call(double x, double y, Algebra s) {
                return s.minus(x, y);
            }
        });
    }
    
    public void testBinaryOperation(BinOp lambda) {
        double x, y;
        x = 1;
        y = 1;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        
        // Test positive and negative numbers with abs(x) > 1. 
        x = 11.11;
        y = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            x = -11.11;
            y = 44.44;
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            x = 11.11;
            y = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            x = -11.11;
            y = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }
        // Swap the relative scales of x and y and test again
        y = 11.11;
        x = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            y = -11.11;
            x = 44.44;
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            y = 11.11;
            x = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            y = -11.11;
            x = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }

        // Test numbers whose abs is equal.
        double abs = 13.1313;
        y = abs;
        x = abs;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            y = abs;
            x = -abs;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict); 
            y = -abs;
            x = abs;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict); 
            y = -abs;
            x = -abs;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }
        
        // Test positive and negative numbers with abs(x) < 1.
        x = 0.11;
        y = 0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            x = -0.11;
            y = 0.44;
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            x = 0.11;
            y = -0.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            x = -0.11;
            y = -0.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }
        // Swap the relative scales of x and y and test again
        y = 0.11;
        x = 0.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            y = -0.11;
            x = 0.44;
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            y = 0.11;
            x = -0.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
            y = -0.11;
            x = -0.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }

        // Test with one value as an infinity.
        x = Double.POSITIVE_INFINITY;
        y = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            x = Double.NEGATIVE_INFINITY;
            y = 44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);  
            x = Double.POSITIVE_INFINITY;
            y = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);    
            x = Double.NEGATIVE_INFINITY;
            y = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }
        
        // Swap the position of the infinities and test again.
        y = Double.POSITIVE_INFINITY;
        x = 44.44;        
        assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        if (supportsNegatives()) {
            y = Double.NEGATIVE_INFINITY;
            x = 44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);  
            y = Double.POSITIVE_INFINITY;
            x = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);    
            y = Double.NEGATIVE_INFINITY;
            x = -44.44;        
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaStrict);
        }
        
        for (int i=0; i<NUM_RANDS; i++) {
            x = Gaussian.nextDouble(0.0, 1000);
            y = Gaussian.nextDouble(0.0, 1000);
            if (!supportsNegatives()) {
                x = Math.abs(x);
                y = Math.abs(y);
            }
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaLoose);  
        }
    }
        
    protected static double compute(BinOp lambda, double x, double y, Algebra s) {
        double compacted = lambda.call(s.fromReal(x), s.fromReal(y), s);
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(compacted));
        return s.toReal(compacted);
    }
    
    @Test
    public void testExp() {
        testUnaryOperation(false, new UnaryOp() {
            @Override
            public double call(double x, Algebra s) {
                return s.exp(x);
            }
        });
    }

    @Test
    public void testLog() {
        testUnaryOperation(true, new UnaryOp() {
            @Override
            public double call(double x, Algebra s) {
                return s.log(x);
            }
        });
    }
    
    public void testUnaryOperation(boolean failOnNegatives, UnaryOp lambda) {
        double x;
        x = 1;
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);
        x = 11.11;   
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);
        x = 0.11;   
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);
        x = Double.POSITIVE_INFINITY;
        assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);    
        
        if (!failOnNegatives && supportsNegatives()) {
            x = -11.11;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);
            x = -0.11;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);
            x = Double.NEGATIVE_INFINITY;
            assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaStrict);    
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
        
        if (!failOnNegatives && supportsNegatives()) {
            for (int i=0; i<NUM_RANDS; i++) {
                x = Gaussian.nextDouble(0.0, 1);                  
                assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaLoose);  
            }
        } else {
            for (int i=0; i<NUM_RANDS; i++) {
                x = Math.abs(Gaussian.nextDouble(0.0, 1));                  
                assertEquals(compute(lambda, x, sReal), compute(lambda, x, sLog), deltaLoose);  
            }
        }
    }
       
    @Test
    public void testTwoZeros() {
        if (supportsNegatives()) {
            // Add positive and negative zero.
            Algebra s = getAlgebra();
            double sum = s.plus(s.times(s.fromReal(-1), s.fromReal(0)), s.fromReal(0));
            assertFalse(s.isNaN(sum));
        }
    }
        
    protected static double compute(UnaryOp lambda, double x, Algebra s) {
        double compacted = lambda.call(s.fromReal(x), s);
        System.out.printf("%0#16x %f\n", Double.doubleToRawLongBits(compacted), s.toReal(compacted));
        return s.toReal(compacted);
    }

}
