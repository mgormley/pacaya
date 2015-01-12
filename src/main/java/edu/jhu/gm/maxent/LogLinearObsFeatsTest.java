package edu.jhu.gm.maxent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.maxent.LogLinearObsFeats.LogLinearObsFeatsPrm;
import edu.jhu.gm.maxent.LogLinearObsFeatsData.LogLinearExample;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.train.AvgBatchObjective;
import edu.jhu.gm.train.CrfObjective;
import edu.jhu.gm.train.CrfObjectiveTest;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.collections.Lists;

public class LogLinearObsFeatsTest {

    @Test
    public void testLogLinearModelTrainDecode() {
        LogLinearObsFeatsData exs = new LogLinearObsFeatsData();
        exs.addEx(30, "y=A", Lists.getList("BIAS", "circle", "solid"));
        exs.addEx(15, "y=B", Lists.getList("BIAS", "circle"));
        exs.addEx(10, "y=C", Lists.getList("BIAS", "solid"));
        exs.addEx(5,  "y=D", Lists.getList("BIAS"));
        List<LogLinearExample> data = exs.getData();
        
        LogLinearObsFeatsPrm prm = new LogLinearObsFeatsPrm();
        prm.includeUnsupportedFeatures = false;
        LogLinearObsFeats td = new LogLinearObsFeats(prm); 
        FgModel model = td.train(exs);
        {
            Pair<String,VarTensor> p = td.decode(model, data.get(0));
            String predLabel = p.get1();
            VarTensor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=A", predLabel);
            double[] goldLogMarg = new double[] { -2.6635044410250623, -2.4546874985293083, -0.1790960208295953,
                    -4.781808684934602 };
            DoubleArrays.exp(goldLogMarg);
            JUnitUtils.assertArrayEquals(goldLogMarg, dist.getValues(), 1e-3);
        }
        {
            Pair<String,VarTensor> p = td.decode(model, data.get(1));
            String predLabel = p.get1();
            VarTensor dist = p.get2();
            System.out.println(Arrays.toString(dist.getValues()));
            assertEquals("y=B", predLabel);
            double[] goldLogMarg = new double[] { -3.4406673404005783, -0.34125259077453896, -1.6728440342006794,
                    -2.668373777179833 };
            DoubleArrays.exp(goldLogMarg);
            JUnitUtils.assertArrayEquals(goldLogMarg, dist.getValues(), 1e-3);
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
    public void testLogLinearModelShapesTwoExamplesLogProbs() {
        boolean logDomain = true;
        testLogLinearModelShapesTwoExamplesHelper(logDomain);
    }

    @Test
    public void testLogLinearModelShapesTwoExamplesProbs() {
        boolean logDomain = false;
        testLogLinearModelShapesTwoExamplesHelper(logDomain);
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
        LogLinearObsFeatsData exs = new LogLinearObsFeatsData();

        exs.addEx(30, "y=A", Lists.getList("BIAS", "circle", "solid"));
        exs.addEx(15, "y=B", Lists.getList("BIAS", "circle"));
        exs.addEx(10, "y=C", Lists.getList("BIAS", "solid"));
        exs.addEx(5,  "y=D", Lists.getList("BIAS"));

        LogLinearObsFeatsPrm prm = new LogLinearObsFeatsPrm();
        prm.includeUnsupportedFeatures = false;
        LogLinearObsFeats td = new LogLinearObsFeats(prm); 
        FgExampleList data = td.getData(exs);

        double[] params = new double[]{3.0, 2.0, 1.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        FgModel model = new FgModel(params.length);
        model.updateModelFromDoubles(params);
        System.out.println(model);
        System.out.println(td.getOfc().toString());
        
        // Test log-likelihood.
        CrfObjective exObj = new CrfObjective(data, CrfObjectiveTest.getInfFactory(logDomain));
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        // Test log-likelihood.
        double ll = obj.getValue(model.getParams())
;
        System.out.println(ll);
        //assertEquals(-2.687, ll, 1e-3);
        
        // Test observed feature counts.
        FeatureVector obsFeats = exObj.getObservedFeatureCounts(model, params);
        assertEquals(10, obsFeats.get(0), 1e-13);
        assertEquals(10, obsFeats.get(1), 1e-13);
        assertEquals(15, obsFeats.get(2), 1e-13);
        assertEquals(15, obsFeats.get(3), 1e-13);
        
        // Test expected feature counts.
        FeatureVector expFeats = exObj.getExpectedFeatureCounts(model, params);
        assertEquals(0.045, expFeats.get(0), 1e-3);
        assertEquals(0.009, expFeats.get(1), 1e-3);
        
        // Test gradient.        
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();                 
        System.out.println(Arrays.toString(gradient));
        JUnitUtils.assertArrayEquals(new double[] { 0.16590575430912835, 0.16651642575232348, 0.24933555564704535,
                0.24941014930579491, -0.4049252957987691, -0.23748187663263962, -0.16349489515194487,
                -0.01031601415740464 }, gradient, 1e-3);
    }
    
    private void testLogLinearModelShapesTwoExamplesHelper(boolean logDomain) {
        LogLinearObsFeatsData exs = new LogLinearObsFeatsData();
        exs.addEx(1, "y=A", Lists.getList("circle"));
        exs.addEx(1, "y=B", Lists.getList("circle"));
        double[] params = new double[]{2.0, 3.0};

        LogLinearObsFeatsPrm prm = new LogLinearObsFeatsPrm();
        prm.includeUnsupportedFeatures = false;
        LogLinearObsFeats td = new LogLinearObsFeats(prm); 
        FgExampleList data = td.getData(exs);
        
        FgModel model = new FgModel(params.length);
        model.updateModelFromDoubles(params);
        
        CrfObjective exObj = new CrfObjective(data, CrfObjectiveTest.getInfFactory(logDomain));
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        assertEquals(1, exs.getAlphabet().size());

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
        assertEquals(1.4621, expFeats.get(1), 1e-3);
        assertEquals(0.5378, expFeats.get(0), 1e-3);
        
        // Test gradient.         
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();  
        double[] expectedGradient = new double[]{1.0 - 0.5378, 1.0 - 1.4621};
        DoubleArrays.scale(expectedGradient, 1.0/2.0);
        JUnitUtils.assertArrayEquals(expectedGradient, gradient, 1e-3);
    }
    
    private void testLogLinearModelShapesOneExampleHelper(boolean logDomain) {
        LogLinearObsFeatsData exs = new LogLinearObsFeatsData();
        exs.addEx(1, "y=A", Lists.getList("circle"));
        exs.addEx(0, "y=B", Lists.getList("circle"));
        double[] params = new double[]{2.0, 3.0};

        LogLinearObsFeatsPrm prm = new LogLinearObsFeatsPrm();
        prm.includeUnsupportedFeatures = true;
        LogLinearObsFeats td = new LogLinearObsFeats(prm);        
        FgExampleList data = td.getData(exs);
        
        FgModel model = new FgModel(params.length);
        model.updateModelFromDoubles(params);

        CrfObjective exObj = new CrfObjective(data, CrfObjectiveTest.getInfFactory(logDomain));
        AvgBatchObjective obj = new AvgBatchObjective(exObj, model, 1);
        
        assertEquals(1, exs.getAlphabet().size());

        // Test log-likelihood.
        double ll = obj.getValue(model.getParams())
;
        System.out.println(ll);
        assertEquals(3*1 - Math.log(Math.exp(3*1) + Math.exp(2*1)), ll, 1e-2);
        
        // Test observed feature counts.
        FeatureVector obsFeats = exObj.getObservedFeatureCounts(model, params);
        assertEquals(0, obsFeats.get(0), 1e-13);
        assertEquals(1, obsFeats.get(1), 1e-13);        
        
        // Test expected feature counts.
        FeatureVector expFeats = exObj.getExpectedFeatureCounts(model, params);
        assertEquals(0.2689, expFeats.get(0), 1e-3);
        assertEquals(0.7310, expFeats.get(1), 1e-3);
        
        // Test gradient.         
        double[] gradient = obj.getGradient(model.getParams()).toNativeArray();
        JUnitUtils.assertArrayEquals(new double[]{-0.2689, 0.2689}, gradient, 1e-3);
    }
    
}
