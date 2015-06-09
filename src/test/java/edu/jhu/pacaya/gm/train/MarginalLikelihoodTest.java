package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.erma.FactorsModule;
import edu.jhu.pacaya.autodiff.erma.FgModelIdentity;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class MarginalLikelihoodTest {

    Algebra s = RealAlgebra.REAL_ALGEBRA;
        
    private FgModel model;
    private FactorGraph fg;
    private VarConfig trainConfig;
    
    @Before
    public void setUp() {
        FactorTemplateList fts = new FactorTemplateList();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SimpleVCObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);       
        
        // Create the FactorGraph
        FgAndVars fgv = CrfTrainerTest.getLinearChainFgWithVars(ofc, obsFe);
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
        
        FactorsModule effm = new FactorsModule(id1, fg, s);
        effm.forward();
        VarTensor[] y = effm.getOutput().f;
        System.out.println(Arrays.deepToString(y));
        assertEquals(Math.exp(2*1), y[0].getValue(0), 1e-1);
        assertEquals(Math.exp(3*2), y[0].getValue(1), 1e-1);
        
        VarTensor[] yAdj = effm.getOutputAdj().f;
        for (int a=0; a<yAdj.length; a++) {
            yAdj[a].fill(5);
        }
        
        effm.backward();
        FgModel grad = id1.getOutputAdj().getModel();
        System.out.println(grad);
        assertEquals(5*Math.exp(2*1)*1, grad.getParams().get(0), 1e-1);
        assertEquals(5*Math.exp(3*2)*2, grad.getParams().get(1), 1e-1);        
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        // This tests ONLY the real semiring, since that is the only supported semiring.
        //model.fill(0.0);
        FgModelIdentity id1 = new FgModelIdentity(model);        
        FgInferencerFactory infFactory = new BruteForceInferencerPrm(LogSemiring.LOG_SEMIRING);
        MarginalLikelihood cll = new MarginalLikelihood(id1, fg, infFactory , trainConfig, 1.0);
        ModuleTestUtils.assertGradientCorrectByFd(cll, 1e-5, 1e-8);
    }

    
}
