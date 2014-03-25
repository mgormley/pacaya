package edu.jhu.util.semiring;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.dist.Gaussian;

public class LogPosNegSemiringTest {

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

    private interface BinOp {
        double call(double x, double y, SemiringExt s);
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
        
        for (int i=0; i<100; i++) {
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

}
