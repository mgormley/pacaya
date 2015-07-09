package edu.jhu.pacaya.gm.model;

import static edu.jhu.pacaya.gm.model.IndexForVcTest.getVar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class IndexForTest {

    @Test
    public void testGetState() {
        Var v0 = getVar(0, 2);
        Var v1 = getVar(1, 3);
        Var v2 = getVar(2, 5);
        Var v3 = getVar(3, 7);

        VarSet vars1 = new VarSet();
        vars1.add(v0);
        vars1.add(v1);
        vars1.add(v2);
        vars1.add(v3);


        VarSet vars2 = new VarSet();
        vars2.add(v1);
        vars2.add(v3);

        IndexFor iter = new IndexFor(vars1, vars2);

        // Just print the values once.
        while (iter.hasNext()) {
            System.out.println(Arrays.toString(iter.getState()));
            System.out.println(iter.next());
        }
        iter.reset();
        for (int b = 0; b < 3; b++) {
            for (int d = 0; d < 7; d++) {
                assertTrue(iter.hasNext());
                System.out.println(Arrays.toString(iter.getState()));
                Assert.assertArrayEquals(new int[]{b, d}, iter.getState());
                int aa = 0;
                int cc = 0;
                assertEquals(aa * 3 * 5 * 7 + b * 5 * 7 + cc * 7 + d, iter.next());
            }
        }
        assertFalse(iter.hasNext());
    }
    
}
