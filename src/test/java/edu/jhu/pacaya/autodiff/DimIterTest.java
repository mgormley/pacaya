package edu.jhu.pacaya.autodiff;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.pacaya.util.semiring.RealAlgebra;

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
    
    @Test
    public void testForTensor() {
        Tensor t = new Tensor(RealAlgebra.getInstance(), 5, 2, 3);
        for (int c=0; c<t.size(); c++) {
            t.setValue(c, c);
        }
        DimIter iter = new DimIter(t.getDims());
        int c = 0; 
        while (iter.hasNext()) {
            int[] idxs = iter.next();
            assertEquals(t.getValue(c), t.get(idxs), 1e-13);
            c++;
        }
    }

}
