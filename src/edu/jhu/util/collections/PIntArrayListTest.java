package edu.jhu.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PIntArrayListTest {

    @Test
    public void testAddGetSize() {
        PIntArrayList list = new PIntArrayList();
        assertEquals(0, list.size());
        
        list.add(getInt(1));
        list.add(getInt(2));
        list.add(getInt(3));
        
        assertEquals(3, list.size());
        
        list.add(getInts(4, 5, 6));
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
        PIntArrayList list = new PIntArrayList();

        for (int i=0; i<50; i++) {
            list.add(getInt(i));
        }
        
        list.trimToSize();

        for (int i=0; i<list.size(); i++) {
            assertEquals(i, toInt(list.get(i)));
        }        
    }
    
    @Test
    public void testClear() {
        PIntArrayList list = new PIntArrayList();
        list.add(getInt(1));
        list.add(getInt(2));
        list.add(getInt(3));   
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

    private int toInt(int d) {
        return d;
    }

    private int[] getInts(int... b) {
        return b;
    }

    private int getInt(int i) {
        return i;
    }

}
