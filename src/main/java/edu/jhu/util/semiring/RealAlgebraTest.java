package edu.jhu.util.semiring;

import static org.junit.Assert.*;

import org.junit.Test;

public class RealAlgebraTest {

    @Test
    public void testEquals() {
        RealAlgebra s1 = new RealAlgebra();
        RealAlgebra s2 = new RealAlgebra();
        assertTrue(s1.equals(s2));
    }

}
