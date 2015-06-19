package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.Beliefs;
import edu.jhu.pacaya.autodiff.erma.FgModelIdentity;
import edu.jhu.pacaya.autodiff.erma.MVecFgModel;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class MarginalLogLikelihoodTest {
        
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
        MarginalLogLikelihood cll = new MarginalLogLikelihood(id1, fg, infFactory , trainConfig);
        Algebra outS = cll.getAlgebra();
        
        Tensor y = cll.forward();
        assertEquals(-5.914, outS.toReal(y.get(0)), 1e-3);
        
        Tensor yAdj = cll.getOutputAdj();
        yAdj.set(outS.fromReal(5.0), 0);
        
        cll.backward();
        FgModel grad = id1.getOutputAdj().getModel();
        System.out.println(grad);
        assertEquals(5*0.574, grad.getParams().get(0), 1e-2);
        assertEquals(5*-0.489, grad.getParams().get(1), 1e-2);        
        assertEquals(5*-0.826, grad.getParams().get(2), 1e-2);        
        assertEquals(5*0.742, grad.getParams().get(3), 1e-2);
    }

    // TODO: This should test an AutoDiffFactor, an AutoDiffGlobalFactor, and a Factor that implements neither.
    @Test
    public void testGradByFiniteDiffs() {
        checkGradByFiniteDiffs(RealAlgebra.getInstance());
        checkGradByFiniteDiffs(LogSignAlgebra.getInstance());
    }

    private void checkGradByFiniteDiffs(Algebra tmpS) {
        // This tests ONLY the real semiring as input, since that is the only supported semiring for FgModelIdentity.
        model.fill(0.0);
        FgModelIdentity id1 = new FgModelIdentity(model);        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        MarginalLogLikelihood cll = new MarginalLogLikelihood(id1, fg, infFactory , trainConfig, tmpS);
        ModuleTestUtils.assertGradientCorrectByFd(cll, 1e-5, 1e-8);
    }
        
    @Test
    public void testGetLogLikelihood() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();   
        double expectedValue = -3.341;
        checkLikelihoodMatches(fgv, expectedValue);
    }

    @Test
    public void testGetLogLikelihoodLatentVars() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVarsLatent();   
        double expectedValue = -1.865;
        checkLikelihoodMatches(fgv, expectedValue);
    }

    private static void checkLikelihoodMatches(FactorGraphsForTests.FgAndVars fgv, double expectedValue) {
        FgModelIdentity mid = new FgModelIdentity(new FgModel(0));        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.getInstance());
        MarginalLogLikelihood obj = new MarginalLogLikelihood(mid, fgv.fg, infFactory, fgv.goldConfig);
        Tensor ll = obj.forward();
        Algebra outS = ll.getAlgebra();
        assertEquals(expectedValue, outS.toReal(ll.get(0)), 1e-3);
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
    
    @Test
    public void testGetGradientLatentVars() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVarsLatent(ofc, obsFe);
        {
            double[] expectedGradient = new double[] {0.75, 0.75, -0.75, -0.75, -0.25, 0.25, -0.25, 0.25, -0.5, 0.5, -0.5, 0.5};        
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 0.0, 0.0);
        }{
            double[] expectedGradient = new double[] {0.75, 0.75, -0.75, -0.75, -0.25, 0.25, -0.25, 0.25, -0.5, 0.5, -0.5, 0.5};        
            checkGradientMatches(fgv, expectedGradient, fts, ofc, obsFe, 1.0, 0.0);
        }
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
        MarginalLogLikelihood obj = new MarginalLogLikelihood(mid, fgv.fg, infFactory, fgv.goldConfig);
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
