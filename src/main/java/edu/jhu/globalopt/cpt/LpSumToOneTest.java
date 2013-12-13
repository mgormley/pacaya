package edu.jhu.globalopt.cpt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.globalopt.cpt.LpSumToOneBuilder.LpStoBuilderPrm;
import edu.jhu.globalopt.cpt.MidpointVarSplitterTest.MockIndexedCpt;
import edu.jhu.globalopt.cpt.Projections.ProjectionsPrm.ProjectionType;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.Prng;


public class LpSumToOneTest {

    @Before
    public void setUp() {
        Prng.seed(Prng.DEFAULT_SEED);
    }
    
    @Test
    public void testSumToOne() throws IloException {
        
        IloCplex cplex = new IloCplex();
        IloLPMatrix lpMatrix = cplex.addLPMatrix("couplingMatrix");
        IndexedCpt icpt = new MockIndexedCpt(2, 3) {
            public String getName(int c, int m) {
                return String.format("param_{%d,%d}", c, m);
            }
        };
        CptBounds bounds = new CptBounds(icpt);

        LpStoBuilderPrm prm = new LpStoBuilderPrm();
        prm.projPrm.type = ProjectionType.NORMALIZE;
        LpSumToOneBuilder sto = new LpSumToOneBuilder(prm);
        sto.init(cplex, lpMatrix, icpt, bounds);
        sto.createModelParamVars();
        sto.addModelParamConstraints();
        
        IloObjective obj = cplex.addMaximize();
        IloLinearNumExpr expr = cplex.linearNumExpr();
        expr.addTerms(new double[] { 1.0, 1.0, 1.0 }, sto.modelParamVars[0]);
        expr.addTerms(new double[] { 1.0, 1.0, 1.0 }, sto.modelParamVars[1]);
        obj.setExpr(expr);
        cplex.solve();
        
        double[][] logProbs = sto.extractRelaxedLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            DoubleArrays.exp(logProbs[c]);
            double sum = DoubleArrays.sum(logProbs[c]);
            System.out.println(sum);
            Assert.assertTrue(Primitives.lte(sum, 1.128, 1e-8));
        }
    }
    
    @Test
    public void testMaxStoCuts() throws IloException {
        
        IloCplex cplex = new IloCplex();
        IloLPMatrix lpMatrix = cplex.addLPMatrix("couplingMatrix");
        IndexedCpt icpt = new MockIndexedCpt(2, 3) {
            public String getName(int c, int m) {
                return String.format("param_{%d,%d}", c, m);
            }
        };
        CptBounds bounds = new CptBounds(icpt);
        
        LpStoBuilderPrm prm = new LpStoBuilderPrm();
        prm.maxStoCuts = 19;
        prm.projPrm.type = ProjectionType.NORMALIZE;
        LpSumToOneBuilder sto = new LpSumToOneBuilder(prm);
        sto.init(cplex, lpMatrix, icpt, bounds);
        sto.createModelParamVars();
        sto.addModelParamConstraints();
        
        IloObjective obj = cplex.addMaximize();
        IloLinearNumExpr expr = cplex.linearNumExpr();
        expr.addTerms(new double[] { 1.0, 1.0, 1.0 }, sto.modelParamVars[0]);
        expr.addTerms(new double[] { 1.0, 1.0, 1.0 }, sto.modelParamVars[1]);
        obj.setExpr(expr);
        cplex.solve();
        
        int nRowsBefore = lpMatrix.getNrows();
        System.out.println("nRowsBefore: " + nRowsBefore);
        Assert.assertEquals(18, nRowsBefore);
        sto.projectModelParamsAndAddCuts();
        int nRowsAfter = lpMatrix.getNrows();
        Assert.assertEquals(19, nRowsAfter);
        
        cplex.solve();
        double[][] logProbs = sto.extractRelaxedLogProbs();
        for (int c=0; c<logProbs.length; c++) {
            DoubleArrays.exp(logProbs[c]);
            double sum = DoubleArrays.sum(logProbs[c]);
            System.out.println(sum);
            Assert.assertTrue(Primitives.lte(sum, 1.128, 1e-8));
        }
    }
}
