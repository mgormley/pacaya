package edu.jhu.hltcoe.util.cplex;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;


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
            Assert.assertEquals(CplexUtils.CPLEX_NEG_INF, CplexUtils.getLowerBound(x1, x2), 1e-13);
            Assert.assertEquals(CplexUtils.CPLEX_POS_INF, CplexUtils.getUpperBound(x1, x2), 1e-13);
        }
    }

    @Test
    public void testGetDualObjectiveValue() throws IloException {
        for (int testVersion=0; testVersion<=4; testVersion++) { 
            System.out.println("Primal test version: " + testVersion);
            runPrimal(testVersion);
            System.out.println();
        }
        for (int testVersion=0; testVersion<=1; testVersion++) { 
            System.out.println("Dual test version: " + testVersion);
            runDual(testVersion);
            System.out.println();
        }
    }
    
    private void runPrimal(int testVersion) throws IloException, UnknownObjectException {
        {
            IloCplex cplex = new IloCplex();

            IloNumVar x1 = cplex.numVar(0, CplexUtils.CPLEX_POS_INF, "x1");
            IloNumVar x2 = cplex.numVar(CplexUtils.CPLEX_NEG_INF, CplexUtils.CPLEX_POS_INF, "x2");
            IloNumVar x3 = cplex.numVar(0, CplexUtils.CPLEX_POS_INF, "x3");
            IloNumVar[] vars = new IloNumVar[] { x1, x2, x3 };

            IloRange c1 = cplex.eq(cplex.scalProd(new double[] { 5, 3, 1 }, vars), 8, "y1");
            IloRange c2 = cplex.le(cplex.scalProd(new double[] { 4, 2, 8 }, vars), 23, "y2");
            IloRange c3 = cplex.le(cplex.scalProd(new double[] { -6, -7, -3 }, vars), -1, "y3");
            IloRange c4 = cplex.le(cplex.prod(1, x1), 4, "y4");
            IloRange[] cons = new IloRange[] { c1, c2, c3, c4 };

            IloLPMatrix mat = cplex.LPMatrix("lpmat");
            mat.addCols(vars);
            mat.addRows(cons);

            if (testVersion == 1) {
                // Add a copy of c2, that is just a ge.
                IloRange c22 = cplex.ge(cplex.scalProd(new double[] { -4, -2, -8 }, vars), -23, "c22");
                mat.addRow(c22);
                mat.removeRow(mat.getIndex(c2));
            } else if (testVersion == 2) {
                // Force x2 to be at its upper bound and nonbasic.
                x2.setUB(2);
            } else if (testVersion == 3) {
                // Add a constraint that has upper and lower bounds.
                IloRange c22 = cplex.range(-23, cplex.scalProd(new double[] { -4, -2, -8 }, vars), -4, "c22");
                mat.addRow(c22);
                mat.removeRow(mat.getIndex(c2));
            } else if (testVersion == 4) {
                // Add a constraint that has upper and lower bounds.
                IloRange c22 = cplex.range(4, cplex.scalProd(new double[] { 4, 2, 8 }, vars), 23, "c22");
                mat.addRow(c22);
                mat.removeRow(mat.getIndex(c2));
            }
            cplex.add(mat);
            cplex.addMaximize(cplex.scalProd(new double[] { 3, 2, 5 }, vars), "obj");

            System.out.println(cplex);
            if (!cplex.solve()) {
                System.out.println("STATUS: " + cplex.getStatus());
                System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
                Assert.fail("Program not solved");
            }
            double dualObjVal = CplexUtils.getDualObjectiveValue(cplex, mat);
            double objVal = cplex.getObjValue();
            System.out.println("values: " + Arrays.toString(cplex.getValues(mat)));
            System.out.println("duals: " + Arrays.toString(cplex.getDuals(mat)));
            System.out.println("varBasis: " + Arrays.toString(cplex.getBasisStatuses(mat.getNumVars())));
            System.out.println("conBasis: " + Arrays.toString(cplex.getBasisStatuses(mat.getRanges())));
            System.out.println(String.format("primal: %f dual: %f", objVal, dualObjVal));
            Assert.assertEquals(objVal, dualObjVal, 1e-13);
        }
    }

    private void runDual(int testVersion) throws IloException, UnknownObjectException {
        {
            IloCplex cplex = new IloCplex();

            IloNumVar x1 = cplex.numVar(CplexUtils.CPLEX_NEG_INF, CplexUtils.CPLEX_POS_INF, "y1");
            IloNumVar x2 = cplex.numVar(0, CplexUtils.CPLEX_POS_INF, "y2");
            IloNumVar x3 = cplex.numVar(0, CplexUtils.CPLEX_POS_INF, "y3");
            IloNumVar x4 = cplex.numVar(0, CplexUtils.CPLEX_POS_INF, "y4");
            IloNumVar[] vars = new IloNumVar[] { x1, x2, x3, x4 };

            IloRange c1 = cplex.ge(cplex.scalProd(new double[] { 5, 4, -6, 1 }, vars), 3, "x1");
            IloRange c2 = cplex.eq(cplex.scalProd(new double[] { 3, 2, -7, 0 }, vars), 2, "x2");
            IloRange c3 = cplex.ge(cplex.scalProd(new double[] { 1, 8, -3, 0 }, vars), 5, "x3");
            IloRange[] cons = new IloRange[] { c1, c2, c3 };

            IloLPMatrix mat = cplex.LPMatrix("lpmat");
            mat.addCols(vars);
            mat.addRows(cons);

            if (testVersion == 1) {
                // Add a copy of c3, that is just a ge.
                IloRange c33 = cplex.le(cplex.scalProd(new double[] { -1, -8, 3, -0 }, vars), -5, "c33");
                mat.addRow(c33);
                mat.removeRow(mat.getIndex(c3));
            }
            cplex.add(mat);
            cplex.addMinimize(cplex.scalProd(new double[] { 8, 23, -1, 4 }, vars), "obj");

            System.out.println(cplex);
            if (!cplex.solve()) {
                System.out.println("STATUS: " + cplex.getStatus());
                System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
                Assert.fail("Program not solved");
            }
            double dualObjVal = CplexUtils.getDualObjectiveValue(cplex, mat);
            double objVal = cplex.getObjValue();
            System.out.println("values: " + Arrays.toString(cplex.getValues(mat)));
            System.out.println("duals: " + Arrays.toString(cplex.getDuals(mat)));
            System.out.println("varBasis: " + Arrays.toString(cplex.getBasisStatuses(mat.getNumVars())));
            System.out.println("conBasis: " + Arrays.toString(cplex.getBasisStatuses(mat.getRanges())));
            System.out.println(String.format("primal: %f dual: %f", objVal, dualObjVal));
            Assert.assertEquals(objVal, dualObjVal, 1e-13);
        }
    }

}
