package edu.jhu.hltcoe.gridsearch.rlt;

import org.junit.Test;
import static org.junit.Assert.*;

public class SymmetricMatrixTest {

    @Test
    public void testSetGet() {
        SymmetricMatrix<Integer> mat = new SymmetricMatrix<Integer>();
        mat.set(0, 0, -2);
        mat.set(1, 0, -3);
        mat.set(2, 1, -4);
        assertEquals(-4, (int) mat.get(2, 1));
        assertEquals(-4, (int) mat.get(1, 2));
        mat.set(1, 2, -5);

        assertEquals(-2, (int) mat.get(0, 0));
        assertEquals(-3, (int) mat.get(1, 0));
        assertEquals(-5, (int) mat.get(2, 1));
        assertEquals(-5, (int) mat.get(1, 2));
    }

}
