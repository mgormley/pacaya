package edu.jhu.util.collections;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PIntDoubleHashMapTest {

    @Test
    public void testPowersOf2() {
        PIntDoubleHashMap map = new PIntDoubleHashMap();
        int start = 0;
        int end = 32;
        for (int i=start; i<end; i++) {
            int key = 2 << i;
            System.out.print(key + " ");
            map.put(key, i);  
        }
        assertEquals(end - start, map.size());
        for (int i=start; i<end; i++) {
            int key = 2 << i;
            assertEquals(i, map.get(key), 1e-13);  
        }
    }

}
