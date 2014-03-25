package edu.jhu.gm.maxent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.train.AvgBatchObjective;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfObjectiveTest;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.JUnitUtils;

public class LogLinearEDsSpeedTest {


    @Test
    public void testLogLinearModelShapesOneExampleProbs() {
        boolean logDomain = false;
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(2);
        model.updateModelFromDoubles(params);
        
        // Test log-likelihood.
        CrfObjective exObj = new CrfObjective(exs.getData(), CrfObjectiveTest.getInfFactory(logDomain));
        DifferentiableFunction obj = new AvgBatchObjective(exObj, model, 1);
        
        // Test average log-likelihood.
        double ll = obj.getValue(model.getParams());
        System.out.println(ll);
        assertEquals(-95.531 / (30.+15.+10.+5.), ll, 1e-3);
        
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
        DoubleArrays.scale(expectedGradient, 1.0 / (30.+15.+10.+5.));
        JUnitUtils.assertArrayEquals(expectedGradient, gradient, 1e-3);
    }
    
}
