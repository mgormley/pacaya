package edu.jhu.lp;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorBuilder.RowFactor;
import edu.jhu.lp.FactorBuilder.RowFactorType;
import edu.jhu.util.JUnitUtils;


public class FactorListTest {

    @Test
    public void testRenormalize() {
        Factor f1 = new RowFactor(1, new int[] {0, 1, 2, 5}, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        Assert.assertEquals(Math.sqrt(16), f1.getL2Norm(), 1e-13);
        Factor f2 = new RowFactor(1, new int[] {0, 1, 2, 5 }, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        Assert.assertEquals(Math.sqrt(16), f2.getL2Norm(), 1e-13);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        
        factors.renormalize();
        
        // Simple check that nothing changes.
        Assert.assertEquals(1, f1.g, 1e-13);
        JUnitUtils.assertArrayEquals(new double[]{1,1,2,3}, f1.G.getValues(), 1e-13);
    }

    @Test
    public void testConvertToLeqFactors() {
        Factor f1 = new RowFactor(1, new int[] {0, 1, 2, 5}, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        Factor f2 = new RowFactor(8, new int[] {0, 1, 3, 7 }, new double[] {4, 5, 6, 7 }, 0, RowFactorType.LOWER, null);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        
        factors = FactorList.convertToLeqFactors(factors);
        System.out.println(factors);
        
        Assert.assertEquals(1, factors.get(0).g, 1e-13);
        Assert.assertArrayEquals(new long[] {0, 1, 2, 5}, factors.get(0).G.getIndices());
        JUnitUtils.assertArrayEquals(new double[] {1, 1, 2, 3 }, factors.get(0).G.getValues(), 1e-13);
        Assert.assertEquals(-1, factors.get(1).g, 1e-13);
        Assert.assertArrayEquals(new long[] {0, 1, 2, 5}, factors.get(1).G.getIndices());
        JUnitUtils.assertArrayEquals(new double[] {-1, -1, -2, -3 }, factors.get(1).G.getValues(), 1e-13);
        
        // Should be unchanged: 
        Assert.assertEquals(8, factors.get(2).g, 1e-13);
        Assert.assertArrayEquals(new long[] {0, 1, 3, 7}, factors.get(2).G.getIndices());
        JUnitUtils.assertArrayEquals(new double[] {4, 5, 6, 7 }, factors.get(2).G.getValues(), 1e-13);
    }
}
