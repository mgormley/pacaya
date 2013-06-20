package edu.jhu.util.collections;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import edu.jhu.util.Timer;

public class PLongDoubleHashMapTest {

    @Test
    public void testPowersOf2() {
        PLongDoubleHashMap map = new PLongDoubleHashMap();
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
        System.out.println("");
    }

    /**
     * This test compares the speed of the Apache (modified) hash map, the GNU
     * Trove hash map, and the Java primitives version.
     * 
     * The Apache version is drastically faster than the other two. GNU Trove is
     * the slowest.
     * 
     * Primitive: 749.0 Trove: 5751.0 Java: 3824.0
     */
    @Test
    public void testSpeed() {
        int max = 1000000;
        {
            Timer timer = new Timer();
            timer.start();
            PLongDoubleHashMap map = new PLongDoubleHashMap();
            for (int i=0; i<max; i++) {
                map.put(i, i);  
            }
            for (int i=0; i<max; i++) {
                double v = map.get(i);
                v *= 2;
            }
    
            for (int i=0; i<max * 2; i++) {
                map.containsKey(i);
            }
            timer.stop();        
            System.out.println("Primitive: " + timer.totMs());
        }
        //        {
        //            Timer timer = new Timer();
        //            timer.start();
        //            TLongDoubleHashMap map = new TLongDoubleHashMap();
        //            for (int i=0; i<max; i++) {
        //                map.put(i, i);  
        //            }
        //            for (int i=0; i<max; i++) {
        //                double v = map.get(i);
        //                v *= 2;
        //            }
        //    
        //            for (int i=0; i<max * 2; i++) {
        //                map.containsKey(i);
        //            }
        //            timer.stop();        
        //            System.out.println("Trove: " + timer.totMs());
        //        }
        {
            Timer timer = new Timer();
            timer.start();
            HashMap<Long,Double> map = new HashMap<Long,Double>();
            for (int i=0; i<max; i++) {
                map.put((long)i, (double)i);  
            }
            for (int i=0; i<max; i++) {
                double v = map.get((long)i);
                v *= 2;
            }

            for (int i=0; i<max * 2; i++) {
                map.containsKey(i);
            }
            timer.stop();  
            System.out.println("Java: " + timer.totMs());
        }
    }

}
