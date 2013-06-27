package edu.jhu.lp;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorBuilder.RowFactor;
import edu.jhu.lp.FactorBuilder.RowFactorType;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Pair;

public class FactorBuilderTest {

    @Test
    public void testGetL2Norm() {
        Factor f = new RowFactor(3, new int[] { 2, 5 }, new double[] { 5, 7 }, 0, RowFactorType.EQ, null);
        Assert.assertEquals(Math.sqrt(5 * 5 + 7 * 7 + 3 * 3), f.getL2Norm(), 1e-13);
    }

    @Test
    public void testGetEqAsLeqPair() {
        Factor f1 = new RowFactor(1, new int[] {0, 1, 2, 5}, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        
        Pair<Factor, Factor> factors = FactorBuilder.getEqFactorAsLeqPair(f1);
                
        Assert.assertEquals(1, factors.get1().g, 1e-13);
        Assert.assertArrayEquals(new long[] {0, 1, 2, 5}, factors.get1().G.getIndices());
        JUnitUtils.assertArrayEquals(new double[] {1, 1, 2, 3 }, factors.get1().G.getValues(), 1e-13);
        Assert.assertEquals(-1, factors.get2().g, 1e-13);
        Assert.assertArrayEquals(new long[] {0, 1, 2, 5}, factors.get2().G.getIndices());
        JUnitUtils.assertArrayEquals(new double[] {-1, -1, -2, -3 }, factors.get2().G.getValues(), 1e-13);
    }
}
