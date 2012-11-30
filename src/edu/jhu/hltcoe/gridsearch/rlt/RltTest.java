package edu.jhu.hltcoe.gridsearch.rlt;

import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltParams;
import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltVarRowFilter;
import edu.jhu.hltcoe.util.CplexUtils;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;

public class RltTest {

    private File tempDir = new File(".");

    @BeforeClass
    public static void classSetUp() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
    }
    
    @AfterClass
    public static void classTearDown() {
        Logger.getRootLogger().setLevel(Level.DEBUG);
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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
        IloLPMatrix rltMat = rlt.getRltMatrix();
        System.out.println(rltMat);
        cplex.add(rltMat);

        // Check the first constraint.
        //assertTrue(rltMat.toString().contains("-576.0*x1 + 768.0*x2 - 36.0*w_{0,0} + 96.0*w_{1,0} - 64.0*w_{1,1} - 2304.0*const <= 0.0"));
        assertContainsRow(rltMat, new double[]{ -576.0, 768.0, -36.0, 96.0, -64.0, -2304.0});
        
        cplex.addMinimize(cplex.sum(cplex.sum(cplex.sum(cplex.prod(-1, rlt.getRltVar(x1, x1)), cplex.prod(-1, rlt.getRltVar(x2, x2))),
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
        double[][] rltVarVals = rlt.getRltVarVals(cplex);

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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
        assertEquals("w_{0,0}", rlt.getRltVar(x1, x1).getName());
        assertEquals("w_{1,0}", rlt.getRltVar(x1, x2).getName());
        assertEquals("w_{1,0}", rlt.getRltVar(x2, x1).getName());
        assertEquals("w_{1,1}", rlt.getRltVar(x2, x2).getName());
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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getConvexConcaveEnvelope());
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        System.out.println(rltMat);

        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});
        
        assertEquals(4, rltMat.getNrows());
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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
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

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        cplex.addMinimize(cplex.sum(cplex.sum(cplex.sum(cplex.prod(-1, rlt.getRltVar(x1, x1)), cplex.prod(-1, rlt.getRltVar(x2, x2))),
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

    @Test
    public void testEqualityConstraints() throws IloException {
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
        IloRange c1 = cplex.eq(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.eq(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloRange[] cons = new IloRange[] { c1, c2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        mat.addRows(cons);

        Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
        IloLPMatrix rltMat = rlt.getRltMatrix();
        cplex.add(rltMat);

        System.out.println(rltMat);

        assertContainsRow(rltMat, new double[]{ -7, -2, 0, 1, 0, 14});
        assertContainsRow(rltMat, new double[]{ -5, -3, 0, 1, 0, 15});
        assertContainsRow(rltMat, new double[]{ 5, 2, 0, -1, 0, -10});
        assertContainsRow(rltMat, new double[]{ 7, 3, 0, -1, 0, -21});
        
        // Assert contains RLT transformed equality constraints.
        assertContainsRow(rltMat, new double[]{ 48, 0, -6, 8, 0, 0});
        assertContainsRow(rltMat, new double[]{ 0, 48, 0, -6, 8, 0});
        assertEquals(14, rltMat.getNrows());
    }    

    @Test
    public void testAddRows() throws IloException {
        Rlt rlt1 = getRlt(false);
        Rlt rlt2 = getRlt(true);
        IloLPMatrix mat1 = rlt1.getRltMatrix();
        IloLPMatrix mat2 = rlt2.getRltMatrix();

        assertEquals(mat1.getNrows(), mat2.getNrows());
        
        assertEquals(mat1.toString(), mat2.toString());
    }

    private Rlt getRlt(boolean addRows) throws IloException {
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(2, 3, "x1");
        IloNumVar x2 = cplex.numVar(5, 7, "x2");
        IloRange c1 = cplex.eq(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.eq(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);
        IloRange c3 = cplex.le(cplex.sum(cplex.prod(-9, x1), cplex.prod(-2, x2)), 48);
        IloRange c4 = cplex.le(cplex.sum(cplex.prod(4, x1), cplex.prod(3, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);

        if (addRows) {
            // Add the first 2.
            IloRange[] cons = new IloRange[] { c1, c3};
            mat.addRows(cons);
            Rlt rlt = new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
            
            // Then add two more.
            List<Integer> newCons = new ArrayList<Integer>();
            newCons.add(mat.addRow(c2));
            newCons.add(mat.addRow(c4));
            rlt.addRows(newCons);
            return rlt;
        } else {
            // Add all four at once.
            IloRange[] cons = new IloRange[] { c1, c3, c2, c4};
            mat.addRows(cons);
            return new Rlt(cplex, mat, RltParams.getFirstOrderRlt());
        }
    }
    
    @Test
    public void testRltFilter() throws IloException {
        IloCplex cplex = new IloCplex();
        // Turn off stdout but not stderr
        // cplex.setOut(null);
        cplex.setParam(IntParam.RootAlg, IloCplex.Algorithm.Primal);

        IloNumVar x1 = cplex.numVar(2, 3, "x1");
        IloNumVar x2 = cplex.numVar(5, 7, "x2");
        IloRange c1 = cplex.eq(cplex.sum(cplex.prod(-6, x1), cplex.prod(8, x2)), 48);
        IloRange c2 = cplex.eq(cplex.sum(cplex.prod(3, x1), cplex.prod(8, x2)), 120);
        IloRange c3 = cplex.le(cplex.sum(cplex.prod(-9, x1), cplex.prod(-2, x2)), 48);
        IloRange c4 = cplex.le(cplex.sum(cplex.prod(4, x1), cplex.prod(3, x2)), 120);

        IloNumVar[] vars = new IloNumVar[] { x1, x2 };
        IloLPMatrix mat = cplex.LPMatrix("lpmat");
        mat.addCols(vars);
        IloRange[] cons = new IloRange[] { c1, c3};
        mat.addRows(cons);
        RltParams prm = RltParams.getFirstOrderRlt();
        prm.filter = new RltVarRowFilter(Arrays.asList(new Pair<IloNumVar,IloNumVar>(x2, x1)));
        Rlt rlt = new Rlt(cplex, mat, prm);
        IloLPMatrix rltMat = rlt.getRltMatrix();
        System.out.println(rltMat);
        assertEquals(11, rltMat.getNrows());

        // Then add two more.
        List<Integer> newCons = new ArrayList<Integer>();
        newCons.add(mat.addRow(c2));
        newCons.add(mat.addRow(c4));
        rlt.addRows(newCons);
        System.out.println(rltMat);
        assertEquals(19, rltMat.getNrows());
        
        double[][] vals = new double[1][];
        rltMat.getCols(rlt.getRltVarIdx(x1, x2), 1, new int[1][], vals);
        for (int i=0; i<vals[0].length; i++) {
            Assert.assertFalse(Utilities.equals(0.0, vals[0][i], 1e-13)); 
        }
    }

    private static void assertContainsRow(IloLPMatrix rltMat, double[] row) {
        // Right now we only handle the 2 variable case.
        int numVars = 2;
        assertEquals(6, row.length);
        double[] newRow = new double[row.length];
        for (int i=0; i<row.length; i++) {
            if (i < numVars) {
                newRow[i] = row[i];
            } else if (i == numVars+1) {
                newRow[i] = row[row.length-1];
            } else {
                newRow[i] = row[i-1];
            }
        }
    }
}
