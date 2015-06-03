package edu.jhu.pacaya.gm.maxent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.train.AvgBatchObjective;
import edu.jhu.pacaya.gm.train.CrfObjective;
import edu.jhu.pacaya.gm.train.CrfObjectiveTest;
import edu.jhu.pacaya.gm.train.SumBatchObjective;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;

public class LogLinearEDsTest {

    // The following 4 tests require support for features of x and y.
    
    @Test
    public void testLogLinearModelShapesLogProbs() {
        // Test with inference in the log-domain.
        Algebra s = LogSemiring.LOG_SEMIRING;        
        testLogLinearModelShapesHelper(s);
    }
    
    @Test
    public void testLogLinearModelShapesProbs() {
        // Test with inference in the prob-domain.
        Algebra s = RealAlgebra.REAL_ALGEBRA;        
        testLogLinearModelShapesHelper(s);
    }

    @Test
    public void testLogLinearModelShapesTwoExamplesLogProbs() {
        Algebra s = LogSemiring.LOG_SEMIRING;
        testLogLinearModelShapesTwoExamplesHelper(s);
    }

    @Test
    public void testLogLinearModelShapesTwoExamplesProbs() {
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        testLogLinearModelShapesTwoExamplesHelper(s);
    }

    @Test
    public void testLogLinearModelShapesOneExampleLogProbs() {
        Algebra s = LogSemiring.LOG_SEMIRING;
        testLogLinearModelShapesOneExampleHelper(s);
    }

    @Test
    public void testLogLinearModelShapesOneExampleProbs() {
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        testLogLinearModelShapesOneExampleHelper(s);
    }

    private void testLogLinearModelShapesHelper(Algebra s) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);
        
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(2);
        model.updateModelFromDoubles(params);

        LogLinearXY maxent = new LogLinearXY(getDefaultLogLinearXYPrm());
        CrfObjective exObj = new CrfObjective(maxent.getData(exs.getData()), CrfObjectiveTest.getInfFactory(s));
        SumBatchObjective obj = new SumBatchObjective(exObj, model, 1);
        
        // Test average log-likelihood.
        double ll = obj.getValue(model.getParams());
        System.out.println(ll);
        assertEquals(-95.531, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = exObj.getObservedFeatureCounts(model, params);
        assertEquals(45, obsFeats.get(0), 1e-13);
        assertEquals(40, obsFeats.get(1), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = exObj.getExpectedFeatureCounts(model, params);
        assertEquals(57.15444760934599, expFeats.get(0), 1e-3);
        assertEquals(52.84782467867294, expFeats.get(1), 1e-3);
        
        // Test gradient.        
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();       
        double[] expectedGradient = new double[]{-12.154447609345993, -12.847824678672943};
        JUnitUtils.assertArrayEquals(expectedGradient, gradient, 1e-3);
    }

    private void testLogLinearModelShapesTwoExamplesHelper(Algebra s) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(1, "circle");
        exs.addEx(1, "solid");
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(2);
        model.updateModelFromDoubles(params);
        
        LogLinearXY maxent = new LogLinearXY(getDefaultLogLinearXYPrm());
        CrfObjective exObj = new CrfObjective(maxent.getData(exs.getData()), CrfObjectiveTest.getInfFactory(s));
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1); // Note: this test uses Avg not Sum.
        
        assertEquals(2, exs.getAlphabet().size());

        // Test average log-likelihood.
        double ll = obj.getValue(model.getParams());        
        System.out.println(ll + " " + Math.exp(ll));
        assertEquals(((3*1 + 2*1) - 2*Math.log((Math.exp(3*1) + Math.exp(2*1)))) / 2.0, ll, 1e-2);
        
        // Test observed feature counts.
        FeatureVector obsFeats = exObj.getObservedFeatureCounts(model, params);
        assertEquals(1, obsFeats.get(0), 1e-13);
        assertEquals(1, obsFeats.get(1), 1e-13);        
        
        // Test expected feature counts.
        FeatureVector expFeats = exObj.getExpectedFeatureCounts(model, params);
        assertEquals(1.4621, expFeats.get(0), 1e-3);
        assertEquals(0.5378, expFeats.get(1), 1e-3);
        
        // Test gradient.         
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();          
        double[] expectedGradient = new double[]{1.0 - 1.4621, 1.0 - 0.5378};
        DoubleArrays.scale(expectedGradient, 1.0/2.0);
        JUnitUtils.assertArrayEquals(expectedGradient, gradient, 1e-3);
    }
    
    private void testLogLinearModelShapesOneExampleHelper(Algebra s) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(1, "circle");
        exs.addEx(0, "solid");
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(2);
        model.updateModelFromDoubles(params);
        
        LogLinearXY maxent = new LogLinearXY(getDefaultLogLinearXYPrm());
        CrfObjective exObj = new CrfObjective(maxent.getData(exs.getData()), CrfObjectiveTest.getInfFactory(s));
        SumBatchObjective obj = new SumBatchObjective(exObj, model, 1);
        
        assertEquals(2, exs.getAlphabet().size());

        // Test log-likelihood.
        double ll = obj.getValue(model.getParams());
        System.out.println(ll);
        assertEquals(3*1 - Math.log(Math.exp(3*1) + Math.exp(2*1)), ll, 1e-2);
        
        // Test observed feature counts.
        FeatureVector obsFeats = exObj.getObservedFeatureCounts(model, params);
        assertEquals(1, obsFeats.get(0), 1e-13);
        assertEquals(0, obsFeats.get(1), 1e-13);        
        
        // Test expected feature counts.
        FeatureVector expFeats = exObj.getExpectedFeatureCounts(model, params);
        assertEquals(0.7310, expFeats.get(0), 1e-3);
        assertEquals(0.2689, expFeats.get(1), 1e-3);
        
        // Test gradient.         
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();       
        JUnitUtils.assertArrayEquals(new double[]{0.2689, -0.2689}, gradient, 1e-3);
    }
    
    public static LogLinearXYPrm getDefaultLogLinearXYPrm() {
        LogLinearXYPrm prm = new LogLinearXYPrm();
        prm.crfPrm.batchOptimizer = null;
        prm.crfPrm.optimizer = new MalletLBFGS(new MalletLBFGSPrm());
        prm.crfPrm.regularizer = new L2(0.0);
        return prm;
    }
    
}
