package edu.jhu.hltcoe.util.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortedIntDoubleVectorTest {

    @Test
    public void testDotProduct() {
        SortedIntDoubleVector v1 = new SortedIntDoubleVector();
        SortedIntDoubleVector v2 = new SortedIntDoubleVector();
        
        v1.set(4,530.8);
        v1.set(49,2.3);
        v1.set(32,2.2);
        v1.set(23,10);
        
        v2.set(3,20.4);
        v2.set(2,1.1);
        v2.set(4,1.1);
        v2.set(23,2.4);
        v2.set(10,0.001);
        v2.set(52, 1.1);
        v2.set(49, 7);
        
        assertEquals(1.1*530.8 + 10*2.4 + 2.3*7, v1.dot(v2), 1e-13);
    }

    @Test
    public void testAdd() {
        SortedIntDoubleVector v1 = new SortedIntDoubleVector();
        v1.set(1, 11.);
        v1.set(3, 33.);
        v1.set(2, 22.);
        
        v1.add(3, 33.);
        v1.add(1, 11.);
        v1.add(2, 22.);
        

		assertEquals(22., v1.get(1), 1e-13);
		assertEquals(44., v1.get(2), 1e-13);
		assertEquals(66., v1.get(3), 1e-13);
    }
    
    @Test
    public void testGetWithNoZeroValues() {
        SortedIntDoubleVector v1 = new SortedIntDoubleVector();
        v1.set(1, 11.);
        v1.set(3, 0.);
        v1.set(2, 22.);
        v1.set(4, 44.);
        v1.set(5, 0.);
        
		assertEquals(11., v1.get(1), 1e-13);
		assertEquals(22., v1.get(2), 1e-13);
		assertEquals(0., v1.get(3), 1e-13);
		assertEquals(44., v1.get(4), 1e-13);
		assertEquals(0., v1.get(5), 1e-13);
		assertEquals(5, v1.getUsed());
		
        SortedIntDoubleVector v2 = SortedIntDoubleVector.getWithNoZeroValues(v1, 1e-13);
        assertEquals(3, v2.getUsed());
		assertEquals(11., v2.get(1), 1e-13);
		assertEquals(22., v2.get(2), 1e-13);
		assertEquals(44., v2.get(4), 1e-13);
    }
    
    @Test
    public void testHadamardProduct() {
        SortedIntDoubleVector v1 = new SortedIntDoubleVector();
        SortedIntDoubleVector v2 = new SortedIntDoubleVector();
        
        v1.set(1, 11.);
        v1.set(3, 0.);
        v1.set(2, 22.);
        v1.set(4, 44.);
        v1.set(5, 0.);
        
        v2.set(1, 11.);
        v2.set(3, 0.);
        v2.set(2, 22.);
        v2.set(4, 0.);
        v2.set(5, 55.);
        
        SortedIntDoubleVector v3 = v1.hadamardProd(v2);

		assertEquals(11*11, v3.get(1), 1e-13);
		assertEquals(22*22, v3.get(2), 1e-13);
		assertEquals(0., v3.get(3), 1e-13);
		assertEquals(0., v3.get(4), 1e-13);
		assertEquals(0., v3.get(5), 1e-13);
    }
    
    @Test
    public void testScale() {
        SortedIntDoubleVector v1 = new SortedIntDoubleVector();
        v1.set(1, 11.);
        v1.set(3, 33.);
        v1.set(2, 22.);

        v1.scale(2.0);
        
		assertEquals(22., v1.get(1), 1e-13);
		assertEquals(44., v1.get(2), 1e-13);
		assertEquals(66., v1.get(3), 1e-13);
    }
    
}    
