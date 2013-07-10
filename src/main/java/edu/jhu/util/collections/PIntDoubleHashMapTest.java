package edu.jhu.util.collections;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import edu.jhu.util.Timer;

public class PIntDoubleHashMapTest {

    @Test
    public void testPowersOf2() {
        PIntDoubleHashMap map = new PIntDoubleHashMap();
        int start = 0;
        int end = Hash.INT_NUM_BITS;
        for (int i=start; i<end; i++) {
            int key = toInt(2) << i;
            System.out.print(key + " ");
            map.put(key, i);  
        }
        assertEquals(end - start, map.size());
        for (int i=start; i<end; i++) {
            int key = toInt(2) << i;
            assertEquals(i, map.get(key), 1e-13);  
        }
        System.out.println("");
    }

    private static int toInt(int d) {
        return (int)d;
    }

}
