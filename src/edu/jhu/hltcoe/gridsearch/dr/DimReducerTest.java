package edu.jhu.hltcoe.gridsearch.dr;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import edu.jhu.hltcoe.gridsearch.dr.DimReducer.DimReducerPrm;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class DimReducerTest {

    @BeforeClass
    public static void classSetUp() {
        BasicConfigurator.configure();
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
        protected DenseDoubleMatrix2D sampleMatrix(int nRows, int nCols) {
            Assert.assertEquals(svals.length, nRows);
            Assert.assertEquals(svals[0].length, nCols);
            return new DenseDoubleMatrix2D(svals);
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

        CplexUtils.assertContainsRow(drMatrix, new double[]{9, 90}, CplexUtils.CPLEX_NEG_INF, -96);
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

        CplexUtils.assertContainsRow(drMatrix, new double[]{-41, 94}, CplexUtils.CPLEX_NEG_INF, 589, 1);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-4, 9}, CplexUtils.CPLEX_NEG_INF, 58, 1);
    }

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

        CplexUtils.assertContainsRow(drMatrix, new double[]{-17, 18}, CplexUtils.CPLEX_NEG_INF, 13, 1);
        CplexUtils.assertContainsRow(drMatrix, new double[]{-22, 19}, CplexUtils.CPLEX_NEG_INF, 131, 1);
    }

    @Test
    public void testFastMultiply() {
        DenseDoubleMatrix2D A = new DenseDoubleMatrix2D(new double[][]{ {1, 2, 3}});
        SparseCCDoubleMatrix2D B = new SparseCCDoubleMatrix2D(new double[][]{ {1}, {2}, {3}});
        
        DenseDoubleMatrix2D C1 = new DenseDoubleMatrix2D(1, 1);
        A.zMult(B, C1);
        DenseDoubleMatrix2D C2 = DimReducer.fastMultiply(A, B);
        
        Assert.assertEquals(C1, C2);
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
