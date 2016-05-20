package edu.jhu.pacaya.gm.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArrayIter3DTest {

    @Test
    public void test() {
        int[][][] indices = new int[][][]{
                { },
                { },
                { { } },
                { {1, 2}, {3, 4} },
                { {5, 6}, { } },
                { },
                };
        ArrayIter3D iter = new ArrayIter3D(indices);
        
        for (int i=0; i<indices.length; i++) {
            for (int j=0; j<indices[i].length; j++) {
                for (int k=0; k<indices[i][j].length; k++) {
                    assertTrue(iter.next());
                    System.out.println(String.format("1: i=%d j=%d k=%d", i, j, k));
                    System.out.println(String.format("2: i=%d j=%d k=%d", iter.i, iter.j, iter.k));
                    System.out.println();
                    assertEquals(i, iter.i);
                    assertEquals(j, iter.j);
                    assertEquals(k, iter.k);
                }
            }
        }
        assertFalse(iter.next());        
    }

}
