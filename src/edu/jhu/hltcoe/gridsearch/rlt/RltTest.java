package edu.jhu.hltcoe.gridsearch.rlt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.Arrays;

import no.uib.cipr.matrix.sparse.FastSparseVector;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltProgram;
import edu.jhu.hltcoe.util.CplexUtils;

public class RltTest {

    private File tempDir = new File(".");

    @BeforeClass
    public static void classSetUp() {
        BasicConfigurator.configure();
        // Logger.getRootLogger().setLevel(Level.TRACE);
    }

    /**
     * Testing the RLT constraints are as given in Sherali & Tuncbilek (1995) in
     * section 3.
     * 
     * @throws IloException
     */
    @Test
    public void testRltConsOnIllustrativeExample() throws IloException {
        runIllustrativeExample(0, 24, -216);
        runIllustrativeExample(0, 8, -180);
        runIllustrativeExample(8, 24, -180);
    }

    private void runIllustrativeExample(double x1Lb, double x1Ub, double expectedObj) throws IloException,
            UnknownObjectException {
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(0, Rlt.CPLEX_POS_INF, "x2");
        IloRange c1 = cplex.le(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.le(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        RltProgram rlt = Rlt.getFirstOrderRlt(cplex, mat);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        IloNumVar[][] rltVars = rlt.getRltVars();
        cplex.add(rltMat);

        System.out.println(rltMat);
        // Check the first constraint.
        //assertTrue(rltMat.toString().contains("-576.0*x1 + 768.0*x2 - 36.0*w_{0,0} + 96.0*w_{1,0} - 64.0*w_{1,1} - 2304.0*const <= 0.0"));
        assertContainsRow(rltMat, new double[]{ -576.0, 768.0, -36.0, 96.0, -64.0, -2304.0});
        
        cplex.addMinimize(cplex.sum(cplex.sum(cplex.sum(cplex.prod(-1, rltVars[0][0]), cplex.prod(-1, rltVars[1][1])),
                cplex.prod(24, x1)), -144), "obj");

        if (tempDir != null) {
            cplex.exportModel(new File(tempDir, "rlt.lp").getAbsolutePath());
        }
        if (!cplex.solve()) {
            System.out.println("STATUS: " + cplex.getStatus());
            System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
            Assert.fail("Program not solved");
        }
        if (tempDir != null) {
            cplex.writeSolution(new File(tempDir, "rlt.sol").getAbsolutePath());
        }

        double[] varVals = cplex.getValues(vars);
        double[][] rltVarVals = CplexUtils.getValues(cplex, rltVars);

        System.out.println(Arrays.toString(varVals));
        System.out.println(Arrays.deepToString(rltVarVals));

        assertEquals(expectedObj, cplex.getObjValue(), 1e-13);

        if (expectedObj == -216) {
            assertEquals(8, varVals[0], 1e-13);
            assertEquals(6, varVals[1], 1e-13);
            assertEquals(192, rltVarVals[0][0], 1e-13);
            assertEquals(48, rltVarVals[1][0], 1e-13);
            assertEquals(72, rltVarVals[1][1], 1e-13);
        }
    }
    
    private void assertContainsRow(IloLPMatrix rltMat, double[] denseRow) throws IloException {
        int nCols = rltMat.getNcols();
        assertTrue(nCols == denseRow.length);
        int nRows = rltMat.getNrows();
        double[] lb = new double[nRows];
        double[] ub = new double[nRows];
        int[][] ind = new int[nRows][];
        double[][] val = new double[nRows][];
        rltMat.getRows(0, nRows, lb, ub, ind, val);
        
        FastSparseVector expectedRow = new FastSparseVector(denseRow);
        
        for (int m=0; m<nRows; m++) {
            FastSparseVector row = new FastSparseVector(ind[m], val[m]);
            //System.out.println(row + "\n" + expectedRow + "\n" + row.equals(expectedRow, 1e-13));
            if (row.equals(expectedRow, 1e-13)) {
                return;
            }
        }
        Assert.fail("Matrix does not contain row: " + Arrays.toString(denseRow));
    }

    @Test
    public void testGetRltVars() throws IloException {
        double x1Lb = 2;
        double x1Ub = 3;
        double x2Lb = 5;
        double x2Ub = 7;
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(x2Lb, x2Ub, "x2");
        
        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);

        RltProgram rlt = Rlt.getFirstOrderRlt(cplex, mat);
        IloNumVar[][] rltVars = rlt.getRltVars();
        assertEquals(rltVars[0][0], rlt.getRltVar(x1, x1));
        assertEquals(rltVars[1][0], rlt.getRltVar(x1, x2));
        assertEquals(rltVars[1][0], rlt.getRltVar(x2, x1));
        assertEquals(rltVars[1][1], rlt.getRltVar(x2, x2));
    }

    @Test
    public void testBoundsOnlyYieldConvexEnvelope() throws IloException {
        double x1Lb = 2;
        double x1Ub = 3;
        double x2Lb = 5;
        double x2Ub = 7;
        
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(x2Lb, x2Ub, "x2");
        
        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);

        RltProgram rlt = Rlt.getFirstOrderRlt(cplex, mat);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        System.out.println(rltMat);

        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});

        assertEquals(10, rltMat.getNrows());
    }

    @Test
    public void testGetConvexEnvelope() throws IloException {
        double x1Lb = 2;
        double x1Ub = 3;
        double x2Lb = 5;
        double x2Ub = 7;
        
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(x2Lb, x2Ub, "x2");
        IloRange c1 = cplex.le(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.le(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        RltProgram rlt = Rlt.getConvexConcaveEnvelope(cplex, mat);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        System.out.println(rltMat);

        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});
        
        assertEquals(10, rltMat.getNrows());
    }
    

    @Test
    public void testUpdateBounds() throws IloException {
        double x1Lb = 2;
        double x1Ub = 3;
        double x2Lb = 5;
        double x2Ub = 7;
        
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(x1Lb, x1Ub, "x1");
        IloNumVar x2 = cplex.numVar(x2Lb, x2Ub, "x2");
        IloRange c1 = cplex.le(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.le(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        RltProgram rlt = Rlt.getFirstOrderRlt(cplex, mat);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        System.out.println(rltMat);

        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});
        assertEquals(21, rltMat.getNrows());
        
        // Leaving the bound unchanged then calling updateBound should have no effect.
        rlt.updateBound(x1, Lu.LOWER);        
        System.out.println(rltMat);
        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});
        assertEquals(21, rltMat.getNrows());

        // Changing the bound then calling updateBound should have effect.
        x1.setUB(11);
        rlt.updateBound(x1, Lu.UPPER);        
        System.out.println(rltMat);
        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -11, 0, 1, 0, 55});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 11, 0, -1, 0, -77});
        assertEquals(21, rltMat.getNrows());
    }
    

    /**
     * Testing the RLT constraints are as given in Sherali & Tuncbilek (1995) in
     * section 3.
     * 
     * @throws IloException
     */
    @Test
    public void testRltConsOnIllustrativeExampleUsingUpdateBound() throws IloException {
        runIllustrativeExample(0, 24, -216);
        runIllustrativeExample(0, 8, -180);
        runIllustrativeExample(8, 24, -180);
        
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(0, 24, "x1");
        IloNumVar x2 = cplex.numVar(0, Rlt.CPLEX_POS_INF, "x2");
        IloRange c1 = cplex.le(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.le(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        RltProgram rlt = Rlt.getFirstOrderRlt(cplex, mat);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        IloNumVar[][] rltVars = rlt.getRltVars();
        cplex.add(rltMat);

        cplex.addMinimize(cplex.sum(cplex.sum(cplex.sum(cplex.prod(-1, rltVars[0][0]), cplex.prod(-1, rltVars[1][1])),
                cplex.prod(24, x1)), -144), "obj");
        
        if (!cplex.solve()) {
            System.out.println("STATUS: " + cplex.getStatus());
            System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
            Assert.fail("Program not solved");
        }
        assertEquals(-216, cplex.getObjValue(), 1e-13);
        

        x1.setLB(8);
        rlt.updateBound(x1, Lu.LOWER);
        if (!cplex.solve()) {
            System.out.println("STATUS: " + cplex.getStatus());
            System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
            Assert.fail("Program not solved");
        }
        assertEquals(-180, cplex.getObjValue(), 1e-13);

        x1.setLB(0);
        x1.setUB(8);
        rlt.updateBound(x1, Lu.LOWER);
        rlt.updateBound(x1, Lu.UPPER);
        if (!cplex.solve()) {
            System.out.println("STATUS: " + cplex.getStatus());
            System.out.println("CPLEX_STATUS: " + cplex.getCplexStatus());
            Assert.fail("Program not solved");
        }
        assertEquals(-180, cplex.getObjValue(), 1e-13);
    }

}
