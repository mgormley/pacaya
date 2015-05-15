package edu.jhu.pacaya.parse.dep;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class EdgeScoresTest {

    private Algebra s = RealAlgebra.REAL_ALGEBRA;

    @Test
    public void testConversionToAndFromTensor() {
        int n = 2;
        EdgeScores es1 = new EdgeScores(n, 3.3);
        es1.setScore(0, 1, 10);
        es1.setScore(1, 0, -10);
        es1.setScore(-1, 0, 20);
        es1.setScore(-1, 1, -20);
                
        Tensor t = es1.toTensor(s);
        assertEquals(10, t.get(0, 1), 1e-13);
        assertEquals(-10, t.get(1, 0), 1e-13);
        // Symmetric entries correspond to the wall entries. 
        assertEquals(20, t.get(0, 0), 1e-13); 
        assertEquals(-20, t.get(1, 1), 1e-13);
        
        EdgeScores es2 = EdgeScores.tensorToEdgeScores(t);
        assertEquals(10, es2.getScore(0, 1), 1e-13);
        assertEquals(-10, es2.getScore(1, 0), 1e-13);
        assertEquals(20, es2.getScore(-1, 0), 1e-13);
        assertEquals(-20, es2.getScore(-1, 1), 1e-13);        
    }

}
