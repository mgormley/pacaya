package edu.jhu.pacaya.util.semiring;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlgebrasTest {

    @Test
    public void testConvertAlgebras() {
        double val = 3.0;
        double logVal = Math.log(val);
        double lsVal = LogSignAlgebra.SINGLETON.fromReal(val);
        
        // Real to other.
        assertEquals(logVal, Algebras.convertAlgebra(val, RealAlgebra.SINGLETON, LogSemiring.SINGLETON), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(val, RealAlgebra.SINGLETON, LogSignAlgebra.SINGLETON), 1e-13);
        
        // Log to other.
        assertEquals(val, Algebras.convertAlgebra(logVal, LogSemiring.SINGLETON, RealAlgebra.SINGLETON), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(logVal, LogSemiring.SINGLETON, LogSignAlgebra.SINGLETON), 1e-13);
        
        // Log-sign to other.
        assertEquals(val, Algebras.convertAlgebra(lsVal, LogSignAlgebra.SINGLETON, RealAlgebra.SINGLETON), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.SINGLETON, LogSemiring.SINGLETON), 1e-13);
        
        // Equal semirings.
        assertEquals(val, Algebras.convertAlgebra(val, RealAlgebra.SINGLETON, RealAlgebra.SINGLETON), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(logVal, LogSemiring.SINGLETON, LogSemiring.SINGLETON), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.SINGLETON, LogSignAlgebra.SINGLETON), 1e-13);        
    }

}
