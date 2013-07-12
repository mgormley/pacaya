package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import edu.jhu.prim.Primitives;
import edu.jhu.util.Timer;

public class LongIntHashMapTest {

    @Test
    public void testPowersOf2() {
        LongIntHashMap map = new LongIntHashMap();
        int start = 0;
        int end = Primitives.LONG_NUM_BITS;
        for (int i=start; i<end; i++) {
            long key = toLong(2) << i;
            System.out.print(key + " ");
            map.put(key, i);  
        }
        assertEquals(end - start, map.size());
        for (int i=start; i<end; i++) {
            long key = toLong(2) << i;
            assertEquals(i, map.get(key));  
        }
        System.out.println("");
    }

    private static long toLong(int d) {
        return (long)d;
    }

}
