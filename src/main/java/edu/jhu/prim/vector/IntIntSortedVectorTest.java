package edu.jhu.prim.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntIntSortedVectorTest {

    @Test
    public void testDotProduct() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        IntIntSortedVector v2 = new IntIntSortedVector();
        v1.set(4, toInt(5308));
        v1.set(49, toInt(23));
        v1.set(32, toInt(22));
        v1.set(23, toInt(10));
        
        v2.set(3, toInt(204));
        v2.set(2, toInt(11));
        v2.set(4, toInt(11));
        v2.set(23, toInt(24));
        v2.set(10, toInt(0001));
        v2.set(52, toInt(11));
        v2.set(49, toInt(7));
        
        assertEquals(11*5308 + 10*24 + 23*7, toInt(v1.dot(v2)));
    }

    @Test
    public void testAdd() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        v1.set(1, toInt(11));
        v1.set(3, toInt(33));
        v1.set(2, toInt(22));
        
        v1.add(3, toInt(33));
        v1.add(1, toInt(11));
        v1.add(2, toInt(22));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testGetWithNoZeroValues() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        v1.set(1, toInt(11));
        v1.set(3, toInt(0));
        v1.set(2, toInt(22));
        v1.set(4, toInt(44));
        v1.set(5, toInt(0));
        
		assertEquals(11, toInt(v1.get(1)));
		assertEquals(22, toInt(v1.get(2)));
		assertEquals(0, toInt(v1.get(3)));
		assertEquals(44, toInt(v1.get(4)));
		assertEquals(0, toInt(v1.get(5)));
		assertEquals(5, v1.getUsed());
		
        IntIntSortedVector v2 = IntIntSortedVector.getWithNoZeroValues(v1);
        assertEquals(3, v2.getUsed());
		assertEquals(11, toInt(v2.get(1)));
		assertEquals(22, toInt(v2.get(2)));
		assertEquals(44, toInt(v2.get(4)));
    }
    
    @Test
    public void testHadamardProduct() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        IntIntSortedVector v2 = new IntIntSortedVector();
        
        v1.set(1, toInt(11));
        v1.set(3, toInt(0));
        v1.set(2, toInt(22));
        v1.set(4, toInt(44));
        v1.set(5, toInt(0));
        
        v2.set(1, toInt(11));
        v2.set(3, toInt(0));
        v2.set(2, toInt(22));
        v2.set(4, toInt(0));
        v2.set(5, toInt(55));
        
        IntIntSortedVector v3 = v1.hadamardProd(v2);

		assertEquals(11*11, toInt(v3.get(1)));
		assertEquals(22*22, toInt(v3.get(2)));
		assertEquals(0, toInt(v3.get(3)));
		assertEquals(0, toInt(v3.get(4)));
		assertEquals(0, toInt(v3.get(5)));
    }
    
    @Test
    public void testScale() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        v1.set(1, toInt(11));
        v1.set(3, toInt(33));
        v1.set(2, toInt(22));

        v1.scale(toInt(2));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testSetAll() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        IntIntSortedVector v2 = new IntIntSortedVector();
        
        v1.set(1, toInt(11));
        v1.set(2, toInt(22));
        v1.set(4, toInt(44));
        
        v2.set(1, toInt(11));
        v2.set(3, toInt(33));
        v2.set(4, toInt(0));
        v2.set(5, toInt(55));
        
        v1.set(v2);

        assertEquals(11, toInt(v1.get(1)));
        assertEquals(0, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(0, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));
    }
    
    @Test
    public void testAddAll() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        IntIntSortedVector v2 = new IntIntSortedVector();
        
        v1.set(1, toInt(11));
        v1.set(2, toInt(22));
        v1.set(4, toInt(44));
        
        v2.set(1, toInt(11));
        v2.set(3, toInt(33));
        v2.set(4, toInt(0));
        v2.set(5, toInt(55));
        
        v1.add(v2);

        assertEquals(22, toInt(v1.get(1)));
        assertEquals(22, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(44, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));        
    }
    
    @Test
    public void testGetElementwiseSum() {
        IntIntSortedVector v1 = new IntIntSortedVector();
        IntIntSortedVector v2 = new IntIntSortedVector();
        
        v1.set(1, toInt(11));
        v1.set(2, toInt(22));
        v1.set(4, toInt(44));
        
        v2.set(1, toInt(11));
        v2.set(3, toInt(33));
        v2.set(4, toInt(0));
        v2.set(5, toInt(55));
        
        IntIntSortedVector v3 = v1.getElementwiseSum(v2);

        assertEquals(22, toInt(v3.get(1)));
        assertEquals(22, toInt(v3.get(2)));
        assertEquals(33, toInt(v3.get(3)));
        assertEquals(44, toInt(v3.get(4)));
        assertEquals(55, toInt(v3.get(5)));        
    }

    private int toInt(int d) {
        return (int)d;
    }

    private int[] toInts(int... b) {
        int[] a = new int[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

}    
