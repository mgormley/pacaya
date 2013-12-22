package edu.jhu.gm.maxent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.maxent.LogLinearData.LogLinearExample;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfObjective.CrfObjectivePrm;
import edu.jhu.gm.train.CrfObjectiveTest;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.collections.Lists;

public class LogLinearTdTest {

    @Test
    public void testLogLinearModelTrainDecode() {
        LogLinearData exs = new LogLinearData();
        exs.addEx(30, "y=A", Lists.getList("BIAS", "circle", "solid"));
        exs.addEx(15, "y=B", Lists.getList("BIAS", "circle"));
        exs.addEx(10, "y=C", Lists.getList("BIAS", "solid"));
        exs.addEx(5,  "y=D", Lists.getList("BIAS"));
        List<LogLinearExample> data = exs.getData();
        
        LogLinearTd td = new LogLinearTd();
        FgModel model = td.train(exs);
        {
            Pair<String,DenseFactor> p = td.decode(model, data.get(0));
            String predLabel = p.get1();
            DenseFactor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=A", predLabel);
            JUnitUtils.assertArrayEquals(new double[] { -0.17909602082959708, -2.454687498529306, -2.6635044410250535,
                    -4.781808684934612 }, dist.getValues(), 1e-3);
        }
        {
            Pair<String,DenseFactor> p = td.decode(model, data.get(1));
            String predLabel = p.get1();
            DenseFactor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=B", predLabel);
            JUnitUtils.assertArrayEquals(new double[] { -1.6728440342006827, -0.3412525907745376, -3.4406673404005708,
                    -2.668373777179843 }, dist.getValues(), 1e-3);
        }
    }
    
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
        LogLinearData exs = new LogLinearData();

        exs.addEx(30, "y=A", Lists.getList("BIAS", "circle", "solid"));
        exs.addEx(15, "y=B", Lists.getList("BIAS", "circle"));
        exs.addEx(10, "y=C", Lists.getList("BIAS", "solid"));
        exs.addEx(5,  "y=D", Lists.getList("BIAS"));
        
        LogLinearTd td = new LogLinearTd();        
        FgExampleList data = td.getData(exs);

        double[] params = new double[]{3.0, 2.0, 1.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        FgModel model = new FgModel(data, false);
        System.out.println(model);
        model.updateModelFromDoubles(params);
        
        // Test log-likelihood.
        CrfObjective obj = new CrfObjective(new CrfObjectivePrm(), model, data, CrfObjectiveTest.getInfFactory(logDomain));
        obj.setPoint(params);
        
        // Test log-likelihood.
        double ll = obj.getValue();
        System.out.println(ll);
        assertEquals(-3.616, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = obj.getObservedFeatureCounts(params);
        assertEquals(30, obsFeats.get(0), 1e-13);
        assertEquals(30, obsFeats.get(1), 1e-13);
        assertEquals(30, obsFeats.get(2), 1e-13);
        assertEquals(15, obsFeats.get(3), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = obj.getExpectedFeatureCounts(params);
        assertEquals(0.248, expFeats.get(0), 1e-3);
        assertEquals(0.217, expFeats.get(1), 1e-3);
        
        // Test gradient.        
        double[] gradient = new double[params.length]; 
        obj.getGradient(gradient);        
                
        JUnitUtils.assertArrayEquals(
                new double[] { 0.4958625913493969, 0.4963669026844305, 0.4995351724875466, 0.06564131448604332,
                        0.06697707783095203, -0.5045766613255576, -0.486189784319966, -0.05692724450988332 }, gradient, 1e-3);
    }
    
    private void testLogLinearModelShapesOneExampleHelper(boolean logDomain) {
        LogLinearData exs = new LogLinearData();
        exs.addEx(1, "y=A", Lists.getList("circle"));
        exs.addEx(0, "y=B", Lists.getList("circle"));
        double[] params = new double[]{3.0, 2.0};

        LogLinearTd td = new LogLinearTd();        
        FgExampleList data = td.getData(exs);
        
        FgModel model = new FgModel(data.getTemplates());
        model.updateModelFromDoubles(params);
        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(logDomain); 
        infFactory = CrfObjectiveTest.getInfFactory(logDomain);
        CrfObjective obj = new CrfObjective(new CrfObjectivePrm(), model, data, infFactory);
        obj.setPoint(params);        
        
        assertEquals(1, exs.getAlphabet().size());

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
