package edu.jhu.util.collections;

import static org.junit.Assert.*;

import org.junit.Test;

public class PLongIntHashMapTest {

    @Test
    public void testPowersOf2() {
        PLongIntHashMap map = new PLongIntHashMap();
        int start = 0;
        int end = 64;
        for (int i=start; i<end; i++) {
            long key = 2l << i;
            System.out.print(key + " ");
            map.put(key, i);  
        }
        assertEquals(end - start, map.size());
        for (int i=start; i<end; i++) {
            long key = 2l << i;
            assertEquals(i, map.get(key), 1e-13);  
        }
    }

}
