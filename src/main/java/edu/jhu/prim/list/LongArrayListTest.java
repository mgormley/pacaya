package edu.jhu.prim.list;

import static org.junit.Assert.*;

import org.junit.Test;

public class LongArrayListTest {

    @Test
    public void testAddGetSize() {
        LongArrayList list = new LongArrayList();
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
        LongArrayList list = new LongArrayList();

        for (int i=0; i<50; i++) {
            list.add(getLong(i));
        }
        
        list.trimToSize();

        for (int i=0; i<list.size(); i++) {
            assertEquals(i, toInt(list.get(i)));
        }        
    }
    
    @Test
    public void testClearGetSize() {
        LongArrayList list = new LongArrayList();
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
    

    @Test
    public void testSet() {
        LongArrayList list = new LongArrayList();
        list.add(getLong(1));
        list.add(getLong(2));
        list.add(getLong(3));   
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
        LongArrayList list = new LongArrayList();
        list.add(getLong(1));
        list.add(getLong(2));
        list.add(getLong(3));
        
        list = new LongArrayList(list);
        
        assertEquals(1, toInt(list.get(0)));
        assertEquals(2, toInt(list.get(1)));
        assertEquals(3, toInt(list.get(2)));
        assertEquals(3, list.size());
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
