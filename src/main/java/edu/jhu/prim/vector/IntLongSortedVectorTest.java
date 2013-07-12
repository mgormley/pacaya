package edu.jhu.prim.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntLongSortedVectorTest {

    @Test
    public void testDotProduct() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        IntLongSortedVector v2 = new IntLongSortedVector();
        v1.set(4, toLong(5308));
        v1.set(49, toLong(23));
        v1.set(32, toLong(22));
        v1.set(23, toLong(10));
        
        v2.set(3, toLong(204));
        v2.set(2, toLong(11));
        v2.set(4, toLong(11));
        v2.set(23, toLong(24));
        v2.set(10, toLong(0001));
        v2.set(52, toLong(11));
        v2.set(49, toLong(7));
        
        assertEquals(11*5308 + 10*24 + 23*7, toInt(v1.dot(v2)));
    }

    @Test
    public void testAdd() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        v1.set(1, toLong(11));
        v1.set(3, toLong(33));
        v1.set(2, toLong(22));
        
        v1.add(3, toLong(33));
        v1.add(1, toLong(11));
        v1.add(2, toLong(22));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testGetWithNoZeroValues() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        v1.set(1, toLong(11));
        v1.set(3, toLong(0));
        v1.set(2, toLong(22));
        v1.set(4, toLong(44));
        v1.set(5, toLong(0));
        
		assertEquals(11, toInt(v1.get(1)));
		assertEquals(22, toInt(v1.get(2)));
		assertEquals(0, toInt(v1.get(3)));
		assertEquals(44, toInt(v1.get(4)));
		assertEquals(0, toInt(v1.get(5)));
		assertEquals(5, v1.getUsed());
		
        IntLongSortedVector v2 = IntLongSortedVector.getWithNoZeroValues(v1);
        assertEquals(3, v2.getUsed());
		assertEquals(11, toInt(v2.get(1)));
		assertEquals(22, toInt(v2.get(2)));
		assertEquals(44, toInt(v2.get(4)));
    }
    
    @Test
    public void testHadamardProduct() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        IntLongSortedVector v2 = new IntLongSortedVector();
        
        v1.set(1, toLong(11));
        v1.set(3, toLong(0));
        v1.set(2, toLong(22));
        v1.set(4, toLong(44));
        v1.set(5, toLong(0));
        
        v2.set(1, toLong(11));
        v2.set(3, toLong(0));
        v2.set(2, toLong(22));
        v2.set(4, toLong(0));
        v2.set(5, toLong(55));
        
        IntLongSortedVector v3 = v1.hadamardProd(v2);

		assertEquals(11*11, toInt(v3.get(1)));
		assertEquals(22*22, toInt(v3.get(2)));
		assertEquals(0, toInt(v3.get(3)));
		assertEquals(0, toInt(v3.get(4)));
		assertEquals(0, toInt(v3.get(5)));
    }
    
    @Test
    public void testScale() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        v1.set(1, toLong(11));
        v1.set(3, toLong(33));
        v1.set(2, toLong(22));

        v1.scale(toLong(2));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testSetAll() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        IntLongSortedVector v2 = new IntLongSortedVector();
        
        v1.set(1, toLong(11));
        v1.set(2, toLong(22));
        v1.set(4, toLong(44));
        
        v2.set(1, toLong(11));
        v2.set(3, toLong(33));
        v2.set(4, toLong(0));
        v2.set(5, toLong(55));
        
        v1.set(v2);

        assertEquals(11, toInt(v1.get(1)));
        assertEquals(0, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(0, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));
    }
    
    @Test
    public void testAddAll() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        IntLongSortedVector v2 = new IntLongSortedVector();
        
        v1.set(1, toLong(11));
        v1.set(2, toLong(22));
        v1.set(4, toLong(44));
        
        v2.set(1, toLong(11));
        v2.set(3, toLong(33));
        v2.set(4, toLong(0));
        v2.set(5, toLong(55));
        
        v1.add(v2);

        assertEquals(22, toInt(v1.get(1)));
        assertEquals(22, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(44, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));        
    }
    
    @Test
    public void testGetElementwiseSum() {
        IntLongSortedVector v1 = new IntLongSortedVector();
        IntLongSortedVector v2 = new IntLongSortedVector();
        
        v1.set(1, toLong(11));
        v1.set(2, toLong(22));
        v1.set(4, toLong(44));
        
        v2.set(1, toLong(11));
        v2.set(3, toLong(33));
        v2.set(4, toLong(0));
        v2.set(5, toLong(55));
        
        IntLongSortedVector v3 = v1.getElementwiseSum(v2);

        assertEquals(22, toInt(v3.get(1)));
        assertEquals(22, toInt(v3.get(2)));
        assertEquals(33, toInt(v3.get(3)));
        assertEquals(44, toInt(v3.get(4)));
        assertEquals(55, toInt(v3.get(5)));        
    }

    private int toInt(long d) {
        return (int)d;
    }

    private long[] toLongs(int... b) {
        long[] a = new long[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private long toLong(int i) {
        return i;
    }
}    
