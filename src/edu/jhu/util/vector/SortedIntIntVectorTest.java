package edu.jhu.util.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class SortedIntIntVectorTest {

    @Test
    public void testDotProduct() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        SortedIntIntVector v2 = new SortedIntIntVector();
        
        v1.set(4,5308);
        v1.set(49,23);
        v1.set(32,22);
        v1.set(23,10);
        
        v2.set(3,204);
        v2.set(2,11);
        v2.set(4,11);
        v2.set(23,24);
        v2.set(10,0001);
        v2.set(52, 11);
        v2.set(49, 7);
        
        assertEquals(11*5308 + 10*24 + 23*7, v1.dot(v2));
    }

    @Test
    public void testAdd() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        v1.set(1, 11);
        v1.set(3, 33);
        v1.set(2, 22);
        
        v1.add(3, 33);
        v1.add(1, 11);
        v1.add(2, 22);
        

		assertEquals(22, v1.get(1));
		assertEquals(44, v1.get(2));
		assertEquals(66, v1.get(3));
    }
    
    @Test
    public void testGetWithNoZeroValues() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        v1.set(1, 11);
        v1.set(3, 0);
        v1.set(2, 22);
        v1.set(4, 44);
        v1.set(5, 0);
        
		assertEquals(11, v1.get(1));
		assertEquals(22, v1.get(2));
		assertEquals(0, v1.get(3));
		assertEquals(44, v1.get(4));
		assertEquals(0, v1.get(5));
		assertEquals(5, v1.getUsed());
		
        SortedIntIntVector v2 = SortedIntIntVector.getWithNoZeroValues(v1);
        assertEquals(3, v2.getUsed());
		assertEquals(11, v2.get(1));
		assertEquals(22, v2.get(2));
		assertEquals(44, v2.get(4));
    }
    
    @Test
    public void testHadamardProduct() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        SortedIntIntVector v2 = new SortedIntIntVector();
        
        v1.set(1, 11);
        v1.set(3, 0);
        v1.set(2, 22);
        v1.set(4, 44);
        v1.set(5, 0);
        
        v2.set(1, 11);
        v2.set(3, 0);
        v2.set(2, 22);
        v2.set(4, 0);
        v2.set(5, 55);
        
        SortedIntIntVector v3 = v1.hadamardProd(v2);

		assertEquals(11*11, v3.get(1));
		assertEquals(22*22, v3.get(2));
		assertEquals(0, v3.get(3));
		assertEquals(0, v3.get(4));
		assertEquals(0, v3.get(5));
    }
    
    @Test
    public void testScale() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        v1.set(1, 11);
        v1.set(3, 33);
        v1.set(2, 22);

        v1.scale(2);
        
		assertEquals(22, v1.get(1));
		assertEquals(44, v1.get(2));
		assertEquals(66, v1.get(3));
    }
    
    @Test
    public void testSetAll() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        SortedIntIntVector v2 = new SortedIntIntVector();
        
        v1.set(1, 11);
        v1.set(2, 22);
        v1.set(4, 44);
        
        v2.set(1, 11);
        v2.set(3, 33);
        v2.set(4, 0);
        v2.set(5, 55);
        
        v1.set(v2);

        assertEquals(11, v1.get(1));
        assertEquals(0, v1.get(2));
        assertEquals(33, v1.get(3));
        assertEquals(0, v1.get(4));
        assertEquals(55, v1.get(5));
    }
    
    @Test
    public void testAddAll() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        SortedIntIntVector v2 = new SortedIntIntVector();
        
        v1.set(1, 11);
        v1.set(2, 22);
        v1.set(4, 44);
        
        v2.set(1, 11);
        v2.set(3, 33);
        v2.set(4, 0);
        v2.set(5, 55);
        
        v1.add(v2);

        assertEquals(22, v1.get(1));
        assertEquals(22, v1.get(2));
        assertEquals(33, v1.get(3));
        assertEquals(44, v1.get(4));
        assertEquals(55, v1.get(5));        
    }
    
    @Test
    public void testGetElementwiseSum() {
        SortedIntIntVector v1 = new SortedIntIntVector();
        SortedIntIntVector v2 = new SortedIntIntVector();
        
        v1.set(1, 11);
        v1.set(2, 22);
        v1.set(4, 44);
        
        v2.set(1, 11);
        v2.set(3, 33);
        v2.set(4, 0);
        v2.set(5, 55);
        
        SortedIntIntVector v3 = v1.getElementwiseSum(v2);

        assertEquals(22, v3.get(1), 1e-13);
        assertEquals(22, v3.get(2), 1e-13);
        assertEquals(33, v3.get(3), 1e-13);
        assertEquals(44, v3.get(4), 1e-13);
        assertEquals(55, v3.get(5), 1e-13);        
    }
}    
