package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;

public class BruteForceInferencerTest {

    @Test
    public void testBpOnTree() {
        FactorGraph fg = FactorGraphTest.getSimpleGraph();
        
        BruteForceInferencer bp = new BruteForceInferencer(fg);
        bp.run();
        
        Factor v0Marg = bp.getMarginals(fg.getVar(0));
        
        JUnitUtils.assertArrayEquals(new double[]{0, 0}, v0Marg.getValues(), 1e-2);
        assertEquals(2.69, bp.getLogPartition(), 1e-2);
    }

}
