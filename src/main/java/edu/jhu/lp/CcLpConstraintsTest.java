package edu.jhu.lp;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorBuilder.RowFactor;
import edu.jhu.lp.FactorBuilder.RowFactorType;
import edu.jhu.prim.matrix.DenseDoubleMatrix;


public class CcLpConstraintsTest {

    @Test
    public void testGetFactorsAsLeqConstraints() {
        Factor f1 = new RowFactor(1, new int[] {0, 2}, new double[] {1, 2}, 0, RowFactorType.UPPER, null);
        Factor f2 = new RowFactor(8, new int[] {0, 1}, new double[] {4, 5}, 1, RowFactorType.UPPER, null);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        CcLpConstraints lc = CcLpConstraints.getLeqFactorsAsLeqConstraints(factors, 3);
        
        double[][] avals = new double[][]{{1, 0, 2}, {4, 5, 0}}; 
        Assert.assertEquals(new DenseDoubleMatrix(avals), lc.A);

        double[][] bvals = new double[][]{{1}, {8}}; 
        Assert.assertEquals(new DenseDoubleMatrix(bvals), lc.b);
    }

    @Test
    public void testGetFactorsAsLeqConstraintsFail() {
        Factor f1 = new RowFactor(1, new int[] {0, 1, 2, 5}, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        Factor f2 = new RowFactor(8, new int[] {0, 1, 3, 7 }, new double[] {4, 5, 6, 7 }, 0, RowFactorType.LOWER, null);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        try {
            CcLpConstraints.getLeqFactorsAsLeqConstraints(factors, 10);
            Assert.fail();    
        } catch (IllegalStateException e) {
            //pass
        }        
    }
    

    @Test
    public void testGetFactorsAsEqConstraints() {
        Factor f1 = new RowFactor(1, new int[] {0, 2}, new double[] {1, 2}, 0, RowFactorType.EQ, null);
        Factor f2 = new RowFactor(8, new int[] {0, 1}, new double[] {4, 5}, 1, RowFactorType.EQ, null);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        CcLpConstraints lc = CcLpConstraints.getEqFactorsAsEqConstraints(factors, 3);

        double[][] avals = new double[][]{{1, 0, 2}, {4, 5, 0}}; 
        Assert.assertEquals(new DenseDoubleMatrix(avals), lc.A);

        double[][] bdvals = new double[][]{{1}, {8}}; 
        Assert.assertEquals(new DenseDoubleMatrix(bdvals), lc.d);
        Assert.assertEquals(new DenseDoubleMatrix(bdvals), lc.b);
    }

    @Test
    public void testGetFactorsAsEqConstraintsFail() {
        Factor f1 = new RowFactor(1, new int[] {0, 1, 2, 5}, new double[] {1, 1, 2, 3 }, 0, RowFactorType.EQ, null);
        Factor f2 = new RowFactor(8, new int[] {0, 1, 3, 7 }, new double[] {4, 5, 6, 7 }, 0, RowFactorType.LOWER, null);
        
        FactorList factors = new FactorList();
        factors.add(f1);
        factors.add(f2);
        try {
            CcLpConstraints.getEqFactorsAsEqConstraints(factors, 10);
            Assert.fail();    
        } catch (IllegalStateException e) {
            //pass
        }        
    }
    
}
