package edu.jhu.gm.maxent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfObjective.CrfObjectivePrm;
import edu.jhu.gm.train.CrfObjectiveTest;
import edu.jhu.util.JUnitUtils;

public class LogLinearEDsTest {

    // TODO: The following 4 tests require support for features of x and y.
    // Currently, we only support features of x conjoined with the values of y.
    @Test
    public void testLogLinearModelShapesLogProbs() {
        // Test with inference in the log-domain.
        boolean logDomain = true;        
        testLogLinearModelShapesHelper(logDomain);
    }
    
    @Test
    public void testLogLinearModelShapesProbs() {
        // Test with inference in the prob-domain.
        boolean logDomain = false;        
        testLogLinearModelShapesHelper(logDomain);
    }
    
    @Test
    public void testLogLinearModelShapesOneExampleLogProbs() {
        boolean logDomain = true;
        testLogLinearModelShapesOneExampleHelper(logDomain);
    }

    @Test
    public void testLogLinearModelShapesOneExampleProbs() {
        boolean logDomain = false;
        testLogLinearModelShapesOneExampleHelper(logDomain);
    }

    private void testLogLinearModelShapesHelper(boolean logDomain) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(exs.getData().getTemplates());
        model.updateModelFromDoubles(params);
        
        // Test log-likelihood.
        CrfObjective obj = new CrfObjective(new CrfObjectivePrm(), model, exs.getData(), CrfObjectiveTest.getInfFactory(logDomain));
        obj.setPoint(params);
        
        // Test log-likelihood.
        double ll = obj.getValue();
        System.out.println(ll);
        assertEquals(-95.531, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertEquals(45, obsFeats.get(0), 1e-13);
        assertEquals(40, obsFeats.get(1), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertEquals(57.15444760934599, expFeats.get(0), 1e-3);
        assertEquals(52.84782467867294, expFeats.get(1), 1e-3);
        
        // Test gradient.        
        double[] gradient = new double[params.length]; 
        obj.getGradient(gradient);        
        JUnitUtils.assertArrayEquals(new double[]{-12.154447609345993, -12.847824678672943}, gradient, 1e-3);
    }
    
    private void testLogLinearModelShapesOneExampleHelper(boolean logDomain) {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(1, "circle");
        exs.addEx(0, "solid");
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(exs.getData().getTemplates());
        model.updateModelFromDoubles(params);
        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(logDomain); 
        infFactory = CrfObjectiveTest.getInfFactory(logDomain);
        CrfObjective obj = new CrfObjective(new CrfObjectivePrm(), model, exs.getData(), infFactory);
        obj.setPoint(params);        
        
        assertEquals(2, exs.getAlphabet().size());

        // Test log-likelihood.
        double ll = obj.getValue();
        System.out.println(ll);
        assertEquals(3*1 - Math.log(Math.exp(3*1) + Math.exp(2*1)), ll, 1e-2);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertEquals(1, obsFeats.get(0), 1e-13);
        assertEquals(0, obsFeats.get(1), 1e-13);        
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertEquals(0.7310, expFeats.get(0), 1e-3);
        assertEquals(0.2689, expFeats.get(1), 1e-3);
        
        // Test gradient.         
        double[] gradient = new double[params.length]; 
        obj.getGradient(gradient);        
        JUnitUtils.assertArrayEquals(new double[]{0.2689, -0.2689}, gradient, 1e-3);
    }
    
}
