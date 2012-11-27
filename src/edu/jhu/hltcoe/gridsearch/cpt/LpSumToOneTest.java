package edu.jhu.hltcoe.gridsearch.cpt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitterTest.MockIndexedCpt;
import edu.jhu.hltcoe.math.Vectors;


public class LpSumToOneTest {

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
        
        LpSumToOneBuilder sto = new LpSumToOneBuilder(new CutCountComputer());
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
            Vectors.exp(logProbs[c]);
            double sum = Vectors.sum(logProbs[c]);
            System.out.println(sum);
        }
    }
    
}
