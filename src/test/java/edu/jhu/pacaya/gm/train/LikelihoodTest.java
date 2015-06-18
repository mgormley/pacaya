package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.autodiff.erma.FactorsModule;
import edu.jhu.pacaya.autodiff.erma.FgModelIdentity;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class LikelihoodTest {
        
    private FgModel model;
    private FactorGraph fg;
    private VarConfig trainConfig;
    
    @Before
    public void setUp() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        // Create the FactorGraph
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars(ofc, obsFe);
        fg = fgv.fg;

        // Create a "gold" assignment of the variables.
        trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);
        
        // Create the training examples.         
        FgExampleMemoryStore exs = new FgExampleMemoryStore();
        exs.add(new LabeledFgExample(fg, trainConfig, obsFe, fts));
        ofc.init(exs);
        
        // Create the Model.
        model = new FgModel(ofc.getNumParams());
        System.out.println("Number of model parameters: " + model.getNumParams());
        model.setRandomStandardNormal();
    }
    
    @Test
    public void testSimple() {
        FgModelIdentity id1 = new FgModelIdentity(model);
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        Likelihood cll = new Likelihood(id1, fg, infFactory , trainConfig);
        Algebra s = cll.getAlgebra();

        Tensor y = cll.forward();
        assertEquals(-5.914, s.toLogProb(y.get(0)), 1e-3);
        
        Tensor yAdj = cll.getOutputAdj();
        yAdj.set(s.fromReal(1.0), 0); // TODO: this should use a different value.
        
        cll.backward();
        FgModel grad = id1.getOutputAdj().getModel();
        System.out.println(grad);
        assertEquals(0.574, grad.getParams().get(0), 1e-2);
        assertEquals(-0.489, grad.getParams().get(1), 1e-3);        
        assertEquals(-0.826, grad.getParams().get(2), 1e-3);        
        assertEquals(0.742, grad.getParams().get(3), 1e-3);
    }
    
    // TODO: This should test an AutoDiffFactor, an AutoDiffGlobalFactor, and a Factor that implements neither.
    @Test
    public void testGradByFiniteDiffs() {
        // This tests ONLY the real semiring, since that is the only supported semiring.
        model.fill(0.0);
        FgModelIdentity id1 = new FgModelIdentity(model);        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        Likelihood cll = new Likelihood(id1, fg, infFactory , trainConfig);
        ModuleTestUtils.assertGradientCorrectByFd(cll, 1e-5, 1e-8);
    }
    
    @Test
    public void testGetLogLikelihood() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();   
        double expectedValue = -3.341;
        checkLikelihoodMatches(fgv, expectedValue);
    }

    @Test(expected=IllegalStateException.class)
    public void testGetLogLikelihoodLatentVars() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVarsLatent();   
        double expectedValue = 123456789;
        checkLikelihoodMatches(fgv, expectedValue);
    }

    private static void checkLikelihoodMatches(FactorGraphsForTests.FgAndVars fgv, double expectedValue) {
        FgModelIdentity mid = new FgModelIdentity(new FgModel(0));        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        Likelihood obj = new Likelihood(mid, fgv.fg, infFactory, fgv.goldConfig);
        Tensor ll = obj.forward();
        Algebra outS = ll.getAlgebra();
        assertEquals(expectedValue, outS.toLogProb(ll.get(0)), 1e-3);
    }
    
    @Test
    public void testGetGradientOneVar() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        FgAndVars fgv = FactorGraphsForTests.getOneVarFgAndVars(ofc, obsFe);
        {
            double[] expectedGradient = new double[] {0.5, -0.5};        
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 0.0, 0.0);
        }{
            double[] expectedGradient = new double[] {0.5, -0.5};        
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 1.0, 0.0);
        }{
            double[] expectedGradient = new double[] {0.731, -0.731};        
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 0.0, 1.0);
        }
    }
    
    @Test
    public void testGetGradient() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars(ofc, obsFe);
        {
            double[] expectedGradient = new double[] { 0.25, 1.25, -0.75, -0.75, -0.5, 0.5, -0.5, 0.5 };
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 0.0, 0.0);
        }{
            double[] expectedGradient = new double[] { 0.25, 1.25, -0.75, -0.75, -0.5, 0.5, -0.5, 0.5 };
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 1.0, 0.0);
        }
    }
    
    @Test(expected=IllegalStateException.class)
    public void testGetGradientLatentVars() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVarsLatent(ofc, obsFe);
        double[] expectedGradient = null;
        checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 0.0, 0.0);
    }

    protected void checkGradientMatches(FgAndVars fgv, double[] expectedGradient, FactorTemplateList fts,
            ObsFeatureConjoiner ofc, SimpleVCObsFeatureExtractor obsFe, double alpha, double beta) {
        // Create the training examples (only to initialize ofc).         
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(new LabeledFgExample(fgv.fg, fgv.goldConfig, obsFe, fts));
        ofc.init(data);
                
        FgModel model = new FgModel(ofc.getNumParams());
        for (int i=0; i<model.getNumParams(); i++) {
            model.getParams().set(i, alpha + beta * i);
        }
        
        FgModelIdentity mid = new FgModelIdentity(model);        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        Likelihood obj = new Likelihood(mid, fgv.fg, infFactory, fgv.goldConfig);
        obj.forward();
        obj.getOutputAdj().set(obj.getAlgebra().one(), 0);
        obj.backward();

        FgModel grad = mid.getOutputAdj().getModel();
        System.out.println("gradient: " + grad);
        for (int i=0; i<model.getNumParams(); i++) {
            assertEquals(expectedGradient[i], grad.getParams().get(i), 1e-3);    
        }
    }

    
}
