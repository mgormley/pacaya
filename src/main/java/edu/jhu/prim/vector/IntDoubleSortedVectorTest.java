package edu.jhu.prim.vector;

import static org.junit.Assert.*;

import org.junit.Test;

public class IntDoubleSortedVectorTest {

    @Test
    public void testDotProduct() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        IntDoubleSortedVector v2 = new IntDoubleSortedVector();
        v1.set(4, toDouble(5308));
        v1.set(49, toDouble(23));
        v1.set(32, toDouble(22));
        v1.set(23, toDouble(10));
        
        v2.set(3, toDouble(204));
        v2.set(2, toDouble(11));
        v2.set(4, toDouble(11));
        v2.set(23, toDouble(24));
        v2.set(10, toDouble(0001));
        v2.set(52, toDouble(11));
        v2.set(49, toDouble(7));
        
        assertEquals(11*5308 + 10*24 + 23*7, toInt(v1.dot(v2)));
    }

    @Test
    public void testAdd() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        v1.set(1, toDouble(11));
        v1.set(3, toDouble(33));
        v1.set(2, toDouble(22));
        
        v1.add(3, toDouble(33));
        v1.add(1, toDouble(11));
        v1.add(2, toDouble(22));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testGetWithNoZeroValues() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        v1.set(1, toDouble(11));
        v1.set(3, toDouble(0));
        v1.set(2, toDouble(22));
        v1.set(4, toDouble(44));
        v1.set(5, toDouble(0));
        
		assertEquals(11, toInt(v1.get(1)));
		assertEquals(22, toInt(v1.get(2)));
		assertEquals(0, toInt(v1.get(3)));
		assertEquals(44, toInt(v1.get(4)));
		assertEquals(0, toInt(v1.get(5)));
		assertEquals(5, v1.getUsed());
		
        IntDoubleSortedVector v2 = IntDoubleSortedVector.getWithNoZeroValues(v1, 1e-13);
        assertEquals(3, v2.getUsed());
		assertEquals(11, toInt(v2.get(1)));
		assertEquals(22, toInt(v2.get(2)));
		assertEquals(44, toInt(v2.get(4)));
    }
    
    @Test
    public void testHadamardProduct() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        IntDoubleSortedVector v2 = new IntDoubleSortedVector();
        
        v1.set(1, toDouble(11));
        v1.set(3, toDouble(0));
        v1.set(2, toDouble(22));
        v1.set(4, toDouble(44));
        v1.set(5, toDouble(0));
        
        v2.set(1, toDouble(11));
        v2.set(3, toDouble(0));
        v2.set(2, toDouble(22));
        v2.set(4, toDouble(0));
        v2.set(5, toDouble(55));
        
        IntDoubleSortedVector v3 = v1.hadamardProd(v2);

		assertEquals(11*11, toInt(v3.get(1)));
		assertEquals(22*22, toInt(v3.get(2)));
		assertEquals(0, toInt(v3.get(3)));
		assertEquals(0, toInt(v3.get(4)));
		assertEquals(0, toInt(v3.get(5)));
    }
    
    @Test
    public void testScale() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        v1.set(1, toDouble(11));
        v1.set(3, toDouble(33));
        v1.set(2, toDouble(22));

        v1.scale(toDouble(2));
        
		assertEquals(22, toInt(v1.get(1)));
		assertEquals(44, toInt(v1.get(2)));
		assertEquals(66, toInt(v1.get(3)));
    }
    
    @Test
    public void testSetAll() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        IntDoubleSortedVector v2 = new IntDoubleSortedVector();
        
        v1.set(1, toDouble(11));
        v1.set(2, toDouble(22));
        v1.set(4, toDouble(44));
        
        v2.set(1, toDouble(11));
        v2.set(3, toDouble(33));
        v2.set(4, toDouble(0));
        v2.set(5, toDouble(55));
        
        v1.set(v2);

        assertEquals(11, toInt(v1.get(1)));
        assertEquals(0, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(0, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));
    }
    
    @Test
    public void testAddAll() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        IntDoubleSortedVector v2 = new IntDoubleSortedVector();
        
        v1.set(1, toDouble(11));
        v1.set(2, toDouble(22));
        v1.set(4, toDouble(44));
        
        v2.set(1, toDouble(11));
        v2.set(3, toDouble(33));
        v2.set(4, toDouble(0));
        v2.set(5, toDouble(55));
        
        v1.add(v2);

        assertEquals(22, toInt(v1.get(1)));
        assertEquals(22, toInt(v1.get(2)));
        assertEquals(33, toInt(v1.get(3)));
        assertEquals(44, toInt(v1.get(4)));
        assertEquals(55, toInt(v1.get(5)));        
    }
    
    @Test
    public void testGetElementwiseSum() {
        IntDoubleSortedVector v1 = new IntDoubleSortedVector();
        IntDoubleSortedVector v2 = new IntDoubleSortedVector();
        
        v1.set(1, toDouble(11));
        v1.set(2, toDouble(22));
        v1.set(4, toDouble(44));
        
        v2.set(1, toDouble(11));
        v2.set(3, toDouble(33));
        v2.set(4, toDouble(0));
        v2.set(5, toDouble(55));
        
        IntDoubleSortedVector v3 = v1.getElementwiseSum(v2);

        assertEquals(22, toInt(v3.get(1)));
        assertEquals(22, toInt(v3.get(2)));
        assertEquals(33, toInt(v3.get(3)));
        assertEquals(44, toInt(v3.get(4)));
        assertEquals(55, toInt(v3.get(5)));        
    }

    private int toInt(double d) {
        return (int)d;
    }

    private double[] toDoubles(int... b) {
        double[] a = new double[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private double toDouble(int i) {
        return i;
    }
}    
