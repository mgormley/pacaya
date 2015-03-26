package edu.jhu.util.semiring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.jhu.util.dist.Gaussian;


public class LogSemiringTest extends AbstractAlgebraTest {

    @Override
    public Algebra getAlgebra() {
        return new LogSemiring();
    }

    @Override
    public boolean supportsNegatives() {
        return false;
    }
    
    @Override
    @Test
    public void testLog() {
        UnaryOp lambda = new UnaryOp() {
            @Override
            public double call(double x, Algebra s) {
                return s.log(x);
            }
        };
        double x = -11.11;
        try {
            compute(lambda, x, sLog);
            fail();
        } catch (Exception e) {
            // pass
        }
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
                
        for (int i=0; i<NUM_RANDS; i++) {
            x = Gaussian.nextDouble(0.0, 1000);
            y = Gaussian.nextDouble(0.0, 1000);
            if (!supportsNegatives()) {
                x = Math.abs(x);
                y = Math.abs(y);
            }
            if (x < y) {
                double tmp = x;
                x = y;
                y = tmp;
            }
            assertEquals(compute(lambda, x, y, sReal), compute(lambda, x, y, sLog), deltaLoose);  
        }
    }

}
