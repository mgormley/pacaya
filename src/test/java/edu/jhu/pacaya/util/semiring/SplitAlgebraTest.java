package edu.jhu.pacaya.util.semiring;

import org.junit.Before;
import org.junit.Test;


public class SplitAlgebraTest extends AbstractAlgebraTest {
    
    private SplitAlgebra s = SplitAlgebra.SINGLETON;
    
    @Before
    public void setUp() {
        deltaStrict = 1e-1;
        deltaLoose = 1e-0;
    }
    
    @Override
    public Algebra getAlgebra() {
        return SplitAlgebra.SINGLETON;
    }
    
    @Override
    protected double getStdDevForRand() {
        // The default of 1000 gives numbers which can't be safely converted to floats.
        return 10;
    }

    @Test
    public void testNegOne() {
        System.out.printf("%f\n", s.toReal(s.fromReal(-1.0)));        
    }
    
    @Test
    public void testPrintouts() {        
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(s.fromReal(1)));
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(s.fromReal(0)));
        System.out.printf("%0#16x\n", Double.doubleToRawLongBits(s.fromReal(-1)));
        
        System.out.printf("%f\n", s.toReal(s.fromReal(-1.0)));
        
        System.out.println((20l << 32) >>> 32);
        System.out.printf("%0#16x\n", (0xFFFFFFFFl << 32) >>> 32);
        
        System.out.println(Float.MAX_VALUE);

//        for (int i=1; i<Float.MAX_VALUE/2; i*=7) {            
//            System.out.printf("%f\n", s.toReal(s.fromReal(i)));
//        }
//        for (int i=-1; i>-Float.MAX_VALUE/2; i*=7) {            
//            System.out.printf("%f\n", s.toReal(s.fromReal(i)));
//        }
    }
    
}
