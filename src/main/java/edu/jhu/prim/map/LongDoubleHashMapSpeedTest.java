package edu.jhu.prim.map;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Test;

import edu.jhu.prim.Primitives;
import edu.jhu.util.Timer;

public class LongDoubleHashMapSpeedTest {

    @Test
    public void testPowersOf2() {
        LongDoubleHashMap map = new LongDoubleHashMap();
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
            LongDoubleHashMap map = new LongDoubleHashMap();
            for (int i=0; i<max; i++) {
                map.put(i, i);  
            }
            for (int i=0; i<max; i++) {
                double v = map.get(i);
                v *= 2;
            }
    
            for (int i=0; i<max * 2; i++) {
                map.contains(i);
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
    
    /** 
     * Output for hashed puts.
     * max: 20
     * Primitive HashMap: 415.0
     * Primitive SortedMap: 322.0
     * max: 200
     * Primitive HashMap: 345.0
     * Primitive SortedMap: 325.0
     * max: 2000                  << Split point
     * Primitive HashMap: 207.0
     * Primitive SortedMap: 237.0
     * max: 20000
     * Primitive HashMap: 270.0
     * Primitive SortedMap: 798.0
     * max: 200000
     * Primitive HashMap: 208.0
     * Primitive SortedMap: 9681.0
     * 
     * Output for in-order puts.
     * 
     *  max: 200
     *  Primitive HashMap: 501.0
     *  Primitive SortedMap: 160.0
     *  max: 2000
     *  Primitive HashMap: 196.0
     *  Primitive SortedMap: 162.0
     *  max: 20000
     *  Primitive HashMap: 288.0
     *  Primitive SortedMap: 168.0
     *  max: 200,000                  << Split point.
     *  Primitive HashMap: 189.0
     *  Primitive SortedMap: 256.0
     */
    @Test
    public void testCompareSortedVsHashMap() {
        int trials = 1000000;
        //int max = 200;
        for (int max=20; max<Math.pow(10, 4)*200; max *= 10) {
            trials /= 10;
            System.out.println("max: " + max);
        {
            Timer timer = new Timer();
            timer.start();
            for (int t = 0; t < trials; t++) {
                LongDoubleHashMap map = new LongDoubleHashMap();
                for (int i = 0; i < max; i++) {
                    map.put(Primitives.hashOfInt(i)%max, i);
                }
            }
            timer.stop();
            System.out.println("Primitive HashMap: " + timer.totMs());
        }
        {
            Timer timer = new Timer();
            timer.start();
            for (int t = 0; t < trials; t++) {
                LongDoubleSortedMap map = new LongDoubleSortedMap();
                for (int i = 0; i < max; i++) {
                    map.put(Primitives.hashOfInt(i)%max, i);
                }
            }
            timer.stop();
            System.out.println("Primitive SortedMap: " + timer.totMs());
        }
        
        }
    }
    
    
    private static long toLong(int d) {
        return (long)d;
    }

}
