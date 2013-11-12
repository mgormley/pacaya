package edu.jhu.gridsearch.dr;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.gridsearch.dr.DimReducer.ConstraintConversion;
import edu.jhu.gridsearch.dr.DimReducer.DimReducerPrm;
import edu.jhu.prim.matrix.DenseDoubleMatrix;
import edu.jhu.prim.matrix.SparseColDoubleMatrix;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Prng;
import edu.jhu.util.cplex.CplexUtils;

public class DimReducerTest {

    @BeforeClass
    public static void classSetUp() {
        Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(123456789101112l);
    }

    private static class MockDimReducer extends DimReducer {

        private double[][] svals;

        public MockDimReducer(DimReducerPrm prm, double[][] svals) {
            super(prm);
            this.svals = svals;
        }

        @Override
        protected DenseDoubleMatrix sampleMatrix(int nRows, int nCols) {
            Assert.assertEquals(svals.length, nRows);
            Assert.assertEquals(svals[0].length, nCols);
            return new DenseDoubleMatrix(svals);
        }

    }

    @Test
    public void testMockDimReducerWithoutRenormalization() throws IloException {
        IloCplex cplex = new IloCplex();
        IloLPMatrix origMatrix = getOrigMatrix(cplex);
        origMatrix.removeRow(3);
        origMatrix.removeRow(0);
        System.out.println(origMatrix);
        IloLPMatrix drMatrix = cplex.LPMatrix("drMat");
        
        double[][] svals = new double[][] {
                {3, 5, 7}
        };
        
        DimReducerPrm prm = new DimReducerPrm();
        prm.drMaxCons = 1;
        prm.renormalize = false;
        DimReducer dr = new MockDimReducer(prm, svals); 
        dr.reduceDimensionality(origMatrix, drMatrix);
        
        Assert.assertEquals(prm.drMaxCons, drMatrix.getNrows());
        Assert.assertEquals(origMatrix.getNcols(), drMatrix.getNcols());

        System.out.println(drMatrix);

        CplexUtils.assertContainsRow(drMatrix, new double[]{-33, -22}, CplexUtils.CPLEX_NEG_INF, -96);
    }

    @Test
    public void testMockDimReducerWithoutRenormalizationSepEqLeq() throws IloException {
        IloCplex cplex = new IloCplex();
        IloLPMatrix origMatrix = getOrigMatrix(cplex);
        System.out.println(origMatrix);
        IloLPMatrix drMatrix = cplex.LPMatrix("drMat");
        
        double[][] svals = new double[][] {
                {3, 5},
        };
        
        DimReducerPrm prm = new DimReducerPrm();
        prm.drMaxCons = 2;
        prm.renormalize = false;
        prm.conversion = ConstraintConversion.SEPARATE_EQ_AND_LEQ;
        DimReducer dr = new MockDimReducer(prm, svals); 
        dr.reduceDimensionality(origMatrix, drMatrix);
        
        Assert.assertEquals(prm.drMaxCons, drMatrix.getNrows());
        Assert.assertEquals(origMatrix.getNcols(), drMatrix.getNcols());

        System.out.println(drMatrix);

        CplexUtils.assertContainsRow(drMatrix, new double[]{-3, 64}, 744, 744);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-7, 9}, CplexUtils.CPLEX_NEG_INF, 744);
    }
    
    @Test
    public void testMockDimReducerWithRenormalization() throws IloException {
        IloCplex cplex = new IloCplex();
        IloLPMatrix origMatrix = getOrigMatrix(cplex);
        IloLPMatrix drMatrix = cplex.LPMatrix("drMat");
        
        double[][] svals = new double[][] {
                {1, 2, 3, 4, 5, 6},
                {0.1, 0.2, 0.3, 0.4, 0.5, 0.6}
        };
        
        DimReducerPrm prm = new DimReducerPrm();
        prm.drMaxCons = 2;
        prm.renormalize = true;
        DimReducer dr = new MockDimReducer(prm, svals); 
        dr.reduceDimensionality(origMatrix, drMatrix);
        
        Assert.assertEquals(prm.drMaxCons, drMatrix.getNrows());
        Assert.assertEquals(origMatrix.getNcols(), drMatrix.getNcols());

        System.out.println(drMatrix);

        CplexUtils.assertContainsRow(drMatrix, new double[]{-21, -17}, CplexUtils.CPLEX_NEG_INF, 589, 1);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-2, -1}, CplexUtils.CPLEX_NEG_INF, 58, 1);
    }

    // TODO: update for DIRICHLET.
    @Test
    public void testFullDimReducer() throws IloException {
        IloCplex cplex = new IloCplex();
        IloLPMatrix origMatrix = getOrigMatrix(cplex);
        IloLPMatrix drMatrix = cplex.LPMatrix("drMat");

        DimReducerPrm prm = new DimReducerPrm();
        prm.drMaxCons = 2;
        DimReducer dr = new DimReducer(prm);
        dr.reduceDimensionality(origMatrix, drMatrix);

        Assert.assertEquals(prm.drMaxCons, drMatrix.getNrows());
        Assert.assertEquals(origMatrix.getNcols(), drMatrix.getNcols());

        System.out.println(drMatrix);

        CplexUtils.assertContainsRow(drMatrix, new double[]{-1, -13}, CplexUtils.CPLEX_NEG_INF, 12, 1);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-13, 1}, CplexUtils.CPLEX_NEG_INF, 131, 1);
    }

    @Test
    public void testFullDimReducerWithIdentity() throws IloException {
        IloCplex cplex = new IloCplex();
        IloLPMatrix origMatrix = getOrigMatrix(cplex);
        IloLPMatrix drMatrix = cplex.LPMatrix("drMat");

        DimReducerPrm prm = new DimReducerPrm();
        prm.useIdentityMatrix = true;
        prm.renormalize = false;
        prm.conversion = ConstraintConversion.SEPARATE_EQ_AND_LEQ;
        DimReducer dr = new DimReducer(prm);
        dr.reduceDimensionality(origMatrix, drMatrix);

        Assert.assertEquals(origMatrix.getNrows(), drMatrix.getNrows());
        Assert.assertEquals(origMatrix.getNcols(), drMatrix.getNcols());

        System.out.println(drMatrix);

        CplexUtils.assertContainsRow(drMatrix, new double[]{-6, 8}, 48, 48, 1);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-9, -2}, CplexUtils.CPLEX_NEG_INF, 48, 1);
    }
    
    @Test
    public void testFastMultiply() {
        DenseDoubleMatrix A = new DenseDoubleMatrix(new double[][]{ {1, 2, 3}});
        SparseColDoubleMatrix B = new SparseColDoubleMatrix(new double[][]{ {1}, {2}, {3}});
        
        DenseDoubleMatrix C1 = new DenseDoubleMatrix(1, 1);
        A.mult(B, C1);
        DenseDoubleMatrix C2 = DimReducer.fastMultiply(A, B);
        
        JUnitUtils.assertArrayEquals(C1.getMatrix(), C2.getMatrix(), 1e-13);
    }
    
    private IloLPMatrix getOrigMatrix(IloCplex cplex) throws IloException {
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(2, 3, "x1");
        IloNumVar x2 = cplex.numVar(5, 7, "x2");
        IloRange c1 = cplex.eq(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.eq(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);
        IloRange c3 = cplex.le(cplex.sum(cplex.prod(-9, x1), cplex.prod(-2, x2)), 48);
        IloRange c4 = cplex.le(cplex.sum(cplex.prod(4, x1), cplex.prod(3, x2)), 120);

        // Add variables.
        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);

        // Add constraints.
        IloRange[] cons = new IloRange[] { c1, c3, c2, c4 };
        mat.addRows(cons);

        return mat;
    }

}
