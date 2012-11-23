package edu.jhu.hltcoe.gridsearch.rlt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.rlt.Rlt.RltProgram;
import edu.jhu.hltcoe.util.CplexUtils;
import edu.jhu.hltcoe.util.JUnitUtils;

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
        IloLPMatrix rltMat = rlt.getLPMatrix();
        IloNumVar[][] rltVars = rlt.getRltVars();
        cplex.add(rltMat);

        IloRange rng0 = rltMat.getRange(0);
        // Check the first constraint.
        double[] lb = new double[1];
        double[] ub = new double[1];
        int[][] ind = new int[1][];
        double[][] val = new double[1][];
        rltMat.getRows(0, 1, lb, ub, ind, val);
        assertEquals(2304.0, ub[0], 1e-13);
        assertArrayEquals(new int[]{0,1,2,3,4}, ind[0]);
        JUnitUtils.assertArrayEquals(new double[]{-576.0, 768.0, -36.0, 96.0, -64.0}, val[0], 1e-13);
        
        cplex.addMinimize(cplex.sum(cplex.sum(cplex.sum(cplex.prod(-1, rltVars[0][0]), cplex.prod(
                -1, rltVars[1][1])), cplex.prod(24, x1)), -144), "obj");

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

}
