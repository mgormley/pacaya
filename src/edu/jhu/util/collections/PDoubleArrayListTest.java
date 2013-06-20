package edu.jhu.util.collections;

import static org.junit.Assert.*;

import org.junit.Test;

public class PDoubleArrayListTest {

    @Test
    public void testAddGetSize() {
        PDoubleArrayList list = new PDoubleArrayList();
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
        PDoubleArrayList list = new PDoubleArrayList();

        for (int i=0; i<50; i++) {
            list.add(getDouble(i));
        }
        
        list.trimToSize();

        for (int i=0; i<list.size(); i++) {
            assertEquals(i, toInt(list.get(i)));
        }        
    }
    
    @Test
    public void testClear() {
        PDoubleArrayList list = new PDoubleArrayList();
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
