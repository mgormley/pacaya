package edu.jhu.prim.list;

import static org.junit.Assert.*;

import org.junit.Test;

public class DoubleArrayListTest {

    @Test
    public void testAddGetSize() {
        DoubleArrayList list = new DoubleArrayList();
        assertEquals(0, list.size());
        
        list.add(getDouble(1));
        list.add(getDouble(2));
        list.add(getDouble(3));
        
        assertEquals(3, list.size());
        
        list.add(getDoubles(4, 5, 6));
        assertEquals(6, list.size());
        
        assertEquals(1, toInt(list.get(0)));
        assertEquals(2, toInt(list.get(1)));
        assertEquals(3, toInt(list.get(2)));
        assertEquals(4, toInt(list.get(3)));
        assertEquals(5, toInt(list.get(4)));
        assertEquals(6, toInt(list.get(5)));
    }
    
    @Test
    public void testAddManyAndTrimToSize() {
        DoubleArrayList list = new DoubleArrayList();

        for (int i=0; i<50; i++) {
            list.add(getDouble(i));
        }
        
        list.trimToSize();

        for (int i=0; i<list.size(); i++) {
            assertEquals(i, toInt(list.get(i)));
        }        
    }
    
    @Test
    public void testClearGetSize() {
        DoubleArrayList list = new DoubleArrayList();
        list.add(getDouble(1));
        list.add(getDouble(2));
        list.add(getDouble(3));   
        try {
            list.get(3);
            fail("Exception should have been thrown.");
        } catch(IndexOutOfBoundsException e) {
            
        }
        assertEquals(3, list.size());
        list.clear();
        assertEquals(0, list.size());
        try {
            list.get(0);
            fail("Exception should have been thrown.");
        } catch(IndexOutOfBoundsException e) {
            
        }
        try {
            list.get(-1);
            fail("Exception should have been thrown.");
        } catch(IndexOutOfBoundsException e) {
            
        }
    }
    

    @Test
    public void testSet() {
        DoubleArrayList list = new DoubleArrayList();
        list.add(getDouble(1));
        list.add(getDouble(2));
        list.add(getDouble(3));   
        try {
            list.set(3, 0);
            fail("Exception should have been thrown.");
        } catch(IndexOutOfBoundsException e) {
            
        } 
        try {
            list.set(-1, 0);
            fail("Exception should have been thrown.");
        } catch(IndexOutOfBoundsException e) {
                        
        }
        
        list.set(0, 3);
        list.set(1, 3);
        assertEquals(3, list.size());

        assertEquals(3, toInt(list.get(0)));
        assertEquals(3, toInt(list.get(1)));
        assertEquals(3, toInt(list.get(2)));
    }

    @Test
    public void testCopyConstructor() {
        DoubleArrayList list = new DoubleArrayList();
        list.add(getDouble(1));
        list.add(getDouble(2));
        list.add(getDouble(3));
        
        list = new DoubleArrayList(list);
        
        assertEquals(1, toInt(list.get(0)));
        assertEquals(2, toInt(list.get(1)));
        assertEquals(3, toInt(list.get(2)));
        assertEquals(3, list.size());
    }
    
    private int toInt(double d) {
        return (int)d;
    }

    private double[] getDoubles(int... b) {
        double[] a = new double[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private double getDouble(int i) {
        return i;
    }

}
