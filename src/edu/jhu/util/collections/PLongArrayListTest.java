package edu.jhu.util.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PLongArrayListTest {

    @Test
    public void testAddGetSize() {
        PLongArrayList list = new PLongArrayList();
        assertEquals(0, list.size());
        
        list.add(getLong(1));
        list.add(getLong(2));
        list.add(getLong(3));
        
        assertEquals(3, list.size());
        
        list.add(getLongs(4, 5, 6));
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
        PLongArrayList list = new PLongArrayList();

        for (int i=0; i<50; i++) {
            list.add(getLong(i));
        }
        
        list.trimToSize();

        for (int i=0; i<list.size(); i++) {
            assertEquals(i, toInt(list.get(i)));
        }        
    }
    
    @Test
    public void testClear() {
        PLongArrayList list = new PLongArrayList();
        list.add(getLong(1));
        list.add(getLong(2));
        list.add(getLong(3));   
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

    private int toInt(long d) {
        return (int)d;
    }

    private long[] getLongs(int... b) {
        long[] a = new long[b.length];
        for (int i=0; i<b.length; i++) {
            a[i] = b[i];
        }
        return a;
    }

    private long getLong(int i) {
        return i;
    }

}
