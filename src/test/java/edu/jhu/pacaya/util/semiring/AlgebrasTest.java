package edu.jhu.pacaya.util.semiring;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlgebrasTest {

    @Test
    public void testConvertAlgebras() {
        double val = 3.0;
        double logVal = Math.log(val);
        double lsVal = LogSignAlgebra.getInstance().fromReal(val);
        
        // Real to other.
        assertEquals(logVal, Algebras.convertAlgebra(val, RealAlgebra.getInstance(), LogSemiring.getInstance()), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(val, RealAlgebra.getInstance(), LogSignAlgebra.getInstance()), 1e-13);
        
        // Log to other.
        assertEquals(val, Algebras.convertAlgebra(logVal, LogSemiring.getInstance(), RealAlgebra.getInstance()), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(logVal, LogSemiring.getInstance(), LogSignAlgebra.getInstance()), 1e-13);
        
        // Log-sign to other.
        assertEquals(val, Algebras.convertAlgebra(lsVal, LogSignAlgebra.getInstance(), RealAlgebra.getInstance()), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.getInstance(), LogSemiring.getInstance()), 1e-13);
        
        // Equal semirings.
        assertEquals(val, Algebras.convertAlgebra(val, RealAlgebra.getInstance(), RealAlgebra.getInstance()), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(logVal, LogSemiring.getInstance(), LogSemiring.getInstance()), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.getInstance(), LogSignAlgebra.getInstance()), 1e-13);        
    }

}
