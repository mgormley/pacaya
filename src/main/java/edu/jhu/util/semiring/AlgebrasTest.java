package edu.jhu.util.semiring;

import static org.junit.Assert.*;

import org.junit.Test;

public class AlgebrasTest {

    @Test
    public void testConvertAlgebras() {
        double val = 3.0;
        double logVal = Math.log(val);
        double lsVal = Algebras.LOG_SIGN_ALGEBRA.fromReal(val);
        
        // Real to other.
        assertEquals(logVal, Algebras.convertAlgebra(val, Algebras.REAL_ALGEBRA, Algebras.LOG_SEMIRING), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(val, Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA), 1e-13);
        
        // Log to other.
        assertEquals(val, Algebras.convertAlgebra(logVal, Algebras.LOG_SEMIRING, Algebras.REAL_ALGEBRA), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(logVal, Algebras.LOG_SEMIRING, Algebras.LOG_SIGN_ALGEBRA), 1e-13);
        
        // Log-sign to other.
        assertEquals(val, Algebras.convertAlgebra(lsVal, Algebras.LOG_SIGN_ALGEBRA, Algebras.REAL_ALGEBRA), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(lsVal, Algebras.LOG_SIGN_ALGEBRA, Algebras.LOG_SEMIRING), 1e-13);
        
        // Equal semirings.
        assertEquals(val, Algebras.convertAlgebra(val, Algebras.REAL_ALGEBRA, Algebras.REAL_ALGEBRA), 1e-13);
        assertEquals(logVal, Algebras.convertAlgebra(logVal, Algebras.LOG_SEMIRING, Algebras.LOG_SEMIRING), 1e-13);
        assertEquals(lsVal, Algebras.convertAlgebra(lsVal, Algebras.LOG_SIGN_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA), 1e-13);        
    }

}
