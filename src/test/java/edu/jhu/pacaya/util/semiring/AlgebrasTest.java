package edu.jhu.pacaya.util.semiring;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AlgebrasTest {

    @Test
    public void testConvertAlgebras() {
        double val = 3.0;
        double logVal = Math.log(val);
        double lsVal = LogSignAlgebra.LOG_SIGN_ALGEBRA.fromReal(val);
        
        // Real to other.
        assertEquals(logVal, Algebras.convertAlgebra(val, RealAlgebra.REAL_ALGEBRA, LogSemiring.LOG_SEMIRING), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(val, RealAlgebra.REAL_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA), 1e-13);
        
        // Log to other.
        assertEquals(val, Algebras.convertAlgebra(logVal, LogSemiring.LOG_SEMIRING, RealAlgebra.REAL_ALGEBRA), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(logVal, LogSemiring.LOG_SEMIRING, LogSignAlgebra.LOG_SIGN_ALGEBRA), 1e-13);
        
        // Log-sign to other.
        assertEquals(val, Algebras.convertAlgebra(lsVal, LogSignAlgebra.LOG_SIGN_ALGEBRA, RealAlgebra.REAL_ALGEBRA), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.LOG_SIGN_ALGEBRA, LogSemiring.LOG_SEMIRING), 1e-13);
        
        // Equal semirings.
        assertEquals(val, Algebras.convertAlgebra(val, RealAlgebra.REAL_ALGEBRA, RealAlgebra.REAL_ALGEBRA), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(logVal, LogSemiring.LOG_SEMIRING, LogSemiring.LOG_SEMIRING), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(lsVal, LogSignAlgebra.LOG_SIGN_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA), 1e-13);        
    }

}
