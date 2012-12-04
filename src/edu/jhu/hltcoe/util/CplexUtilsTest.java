package edu.jhu.hltcoe.util;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.rlt.Rlt;

public class CplexUtilsTest {

    // TODO: move this test to a more appropriate place.
    @Test
    public void testQuadraticObjectiveInCplex() throws IloException {
        System.out.println("Trying to solve quadratic");
        IloCplex cplex = new IloCplex();

        IloNumVar thetaVar = cplex.numVar(-20, 0, "theta");
        IloNumVar edgeVar = cplex.numVar(0, 1, "edge");

        cplex.addMinimize(cplex.prod(edgeVar, thetaVar), "obj");

        cplex.exportModel(new File("quad.lp").getAbsolutePath());

        try {
            cplex.solve();
            Assert.fail();
        } catch (Exception e) {
            // pass
        }
    }

    @Test
    public void testCplexSetNzs() throws IloException {
        double x1Lb = 2;
        double x1Ub = 3;
        double x2Lb = 5;
        double x2Ub = 7;

        IloCplex cplex = new IloCplex();

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(x2Lb, x2Ub, "x2");
        IloRange c1 = cplex.le(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.le(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        assertEquals(-6, mat.getNZ(0, 0), 1e-13);
        assertEquals(8, mat.getNZ(0, 1), 1e-13);
        mat.setNZs(new int[] { 0 }, new int[] { 1 }, new double[] { 9 });
        assertEquals(-6, mat.getNZ(0, 0), 1e-13);
        assertEquals(9, mat.getNZ(0, 1), 1e-13);
        mat.setNZs(new int[] { 0, 0 }, new int[] { 0, 1 }, new double[] { 0, 10 });
        assertEquals(0, mat.getNZ(0, 0), 1e-13);
        assertEquals(10, mat.getNZ(0, 1), 1e-13);
        // We might expect 3 here, but it seems zero can be a "non-zero"
        // coefficient.
        assertEquals(4, mat.getNNZs());

        // Impossible to set non-zero coefficients of rows or columns that
        // haven't been added.
        try {
            mat.setNZ(2, 3, 33);
            Assert.fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // pass
        }
        try {
            mat.setNZs(new int[] { 3 }, new int[] { 2 }, new double[] { 33 });
            Assert.fail();
        } catch (NullPointerException e) {
            // pass
        }
    }

    @Test
    public void testQuadraticBounds() throws IloException {
        IloCplex cplex = new IloCplex();
        {
            IloNumVar x1 = cplex.numVar(-7, -2, "x1");
            IloNumVar x2 = cplex.numVar(5, 11, "x2");
            Assert.assertEquals(-77, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(-10, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
        {
            IloNumVar x1 = cplex.numVar(2, 7, "x1");
            IloNumVar x2 = cplex.numVar(5, 11, "x2");
            Assert.assertEquals(10, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(77, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
        {
            IloNumVar x1 = cplex.numVar(-2, 7, "x1");
            IloNumVar x2 = cplex.numVar(5, 11, "x2");
            Assert.assertEquals(-22, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(77, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
        {
            IloNumVar x1 = cplex.numVar(-7, -2, "x1");
            IloNumVar x2 = cplex.numVar(-11, -5, "x2");
            Assert.assertEquals(10, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(77, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
        {
            IloNumVar x1 = cplex.numVar(-1e20, 1e20, "x1");
            IloNumVar x2 = cplex.numVar(-11, -5, "x2");
            Assert.assertEquals(Rlt.CPLEX_NEG_INF, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(Rlt.CPLEX_POS_INF, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
    }
}
