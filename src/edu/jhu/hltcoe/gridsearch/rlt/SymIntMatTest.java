package edu.jhu.hltcoe.gridsearch.rlt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class SymIntMatTest {

    @Test
    public void testSetGet() {
        SymIntMat mat = new SymIntMat();
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

    @Test
    public void testIncrementAll() {
        SymIntMat mat = new SymIntMat();
        mat.set(0, 0, -2);
        mat.set(1, 0, -3);
        mat.set(2, 1, -4);
        
        mat.incrementAll(3);
        
        assertEquals(1, (int) mat.get(0, 0));
        assertEquals(0, (int) mat.get(1, 0));
        assertEquals(-1, (int) mat.get(2, 1));
    }
    
    @Test
    public void testSetAll() {
        SymIntMat mat1 = new SymIntMat();
        mat1.set(0, 0, -2);
        mat1.set(1, 0, -3);
        SymIntMat mat2 = new SymIntMat();
        mat2.set(2, 1, -4);
        mat2.set(2, 2, -5);

        mat1.setAll(mat2);
        
        assertEquals(-2, (int) mat1.get(0, 0));
        assertEquals(-3, (int) mat1.get(0, 1));
        assertEquals(-4, (int) mat1.get(1, 2));
        assertEquals(-5, (int) mat1.get(2, 2));
    }
}
