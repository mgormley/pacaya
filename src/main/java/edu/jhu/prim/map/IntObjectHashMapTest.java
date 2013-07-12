package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;

public class IntObjectHashMapTest {

    @Test
    public void testPowersOf2() {
        IntObjectHashMap<Integer> map = new IntObjectHashMap<Integer>();
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
    
    @Test
    public void testKeys() {
        IntObjectHashMap<Integer> map = new IntObjectHashMap<Integer>();
        map.put(2, 22);
        map.put(5, 55);
        map.put(7, 77);
        
        JUnitUtils.assertArrayEquals(new int[]{2, 5, 7}, map.keys());
    }

}
