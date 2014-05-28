package edu.jhu.autodiff;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class DimIterTest {

    @Test
    public void testSimple2() {
        DimIter iter = new DimIter(2, 3);
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                assertTrue(iter.hasNext());
                int[] states = iter.next();
                System.out.println(Arrays.toString(states));
                assertArrayEquals(new int[]{i, j} , states);      
            }
        }
        assertTrue(!iter.hasNext()); 
        try {
            iter.next();
            fail("Exception should have been thrown.");
        } catch(Exception e) {
            // pass
        }
    }
    
    @Test
    public void testSimple3() {
        DimIter iter = new DimIter(5, 2, 3);
        for (int h=0; h<5; h++) {
            for (int i=0; i<2; i++) {
                for (int j=0; j<3; j++) {
                    assertTrue(iter.hasNext());
                    int[] states = iter.next();
                    System.out.println(Arrays.toString(states));
                    assertArrayEquals(new int[]{h, i, j} , states);      
                }
            }
        }
        assertTrue(!iter.hasNext()); 
        try {
            iter.next();
            fail("Exception should have been thrown.");
        } catch(Exception e) {
            // pass
        }
    }

}
