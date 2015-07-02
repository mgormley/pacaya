package edu.jhu.pacaya.gm.train;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.SGD;
import edu.jhu.hlt.optimize.SGD.SGDPrm;
import edu.jhu.hlt.optimize.function.Regularizer;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.pacaya.autodiff.erma.EmpiricalRiskTest;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.autodiff.erma.L2Distance.MeanSquaredErrorFactory;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeExpFamFactor;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureExtractor;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.maxent.LogLinearEDs;
import edu.jhu.pacaya.gm.maxent.LogLinearXY;
import edu.jhu.pacaya.gm.maxent.LogLinearXYData;
import edu.jhu.pacaya.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.pacaya.gm.model.ExpFamFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.FgModelTest;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.pacaya.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.pacaya.gm.train.CrfTrainer.Trainer;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.random.Prng;

public class CrfTrainerTest {

    @Before
    public void setUp() {
        Prng.seed(123456789101112l);
    }
    
    @Test 
    public void testLogLinearModelShapes() {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{-3.0, 2.0};
        FgModel model = new FgModel(params.length);
        model.updateModelFromDoubles(params);
        
        LogLinearXYData data = exs.getData();
        LogLinearXY maxent = new LogLinearXY(new LogLinearXYPrm());
        
        model = train(model, maxent.getData(data));
        
        // Note: this used to be 1.093.
        JUnitUtils.assertArrayEquals(new double[]{1.098, 0.693}, FgModelTest.getParams(model), 1e-3);
    }
    
    @Test 
    public void testLogLinearModelShapesErma() {
        LogLinearXYData xyData = new LogLinearXYData();
        List<String>[] fvs;
        fvs = new List[]{ QLists.getList("x=A,y=A"), QLists.getList("x=A,y=B") };
        xyData.addExStrFeats(1.0, "x=A", "y=A", fvs);
        fvs = new List[]{ QLists.getList("x=B,y=A"), QLists.getList("x=B,y=B") };
        xyData.addExStrFeats(1.0, "x=B", "y=B", fvs);        
        LogLinearXY xy = new LogLinearXY(new LogLinearXYPrm());
        FgExampleList data = xy.getData(xyData);
          
        
        //double[] params = new double[]{-3., -2., -1.0};
        double[] params = new double[]{0, 0, 0, 0};
        
        FgModel model = new FgModel(params.length);
        
        Regularizer r = null; //new L2(100);

        model.updateModelFromDoubles(params);
        model = train(model, data, r, true);        
        double[] params1 = FgModelTest.getParams(model);
        
        // ERMA should get the same answer as the CLL training in this case.
        model.updateModelFromDoubles(params);
        model = trainErma(model, data, r, true);  
        double[] params2 = FgModelTest.getParams(model);
        
        System.out.println(DoubleArrays.toString( params1, "%.3f"));
        System.out.println(DoubleArrays.toString( params2, "%.3f"));
        
        JUnitUtils.assertArrayEquals(new double[]{0.166, -0.166, -0.166, 0.166}, params1, 1e-3);
        JUnitUtils.assertArrayEquals(new double[]{0.253, -0.253, -0.253, 0.253}, params2, 1e-3);
        //MSE: JUnitUtils.assertArrayEquals(new double[]{0.145, -0.145, -0.145, 0.145}, params2, 1e-3);
    }

    @Test
    public void testTrainNoLatentVarsSgd() {
        checkTrainNoLatentVars(true);
    }
    
    @Test
    public void testTrainNoLatentVarsLbfgs() {
        checkTrainNoLatentVars(false);
    }
    
    public void checkTrainNoLatentVars(boolean sgd) {
        // Boiler plate feature extraction code.
        FactorTemplateList fts = new FactorTemplateList();        
        ObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);

        // Create the factor graph.
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars(ofc, obsFe);

        // Create a "gold" assignment of the variables.
        VarConfig trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);

        // Create a set of examples, consisting of ONLY ONE example.
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(new LabeledFgExample(fgv.fg, trainConfig, obsFe, fts));
        ofc.init(data);
        FgModel model = new FgModel(ofc.getNumParams());

        // Train the model.
        model = train(model, data, null, sgd);
        
        // Assertions:
        System.out.println(model);
        System.out.println(fts);
        System.out.println(DoubleArrays.toString(FgModelTest.getParams(model), "%.2f"));
        double[] expected;
        if (sgd) {
            expected = new double[]{0.19, 0.11, -0.40, 0.11, -0.26, 0.34, -0.32, 0.25};
        } else {
            expected = new double[]{2.85, 0.99, -4.82, 0.98, -6.33, 9.25, -6.55, 3.63};
            // Due to floating point precision, we used to run an extra iteration of LBFGS
            // and got the following answer:
            //
            //expected = new double[]{3.32, 1.05, -5.43, 1.05, -7.09, 10.45, -7.26, 3.89};
        }
        JUnitUtils.assertArrayEquals(expected, FgModelTest.getParams(model), 1e-2);

        // OLD WAY:
        //        assertEquals(4.79, getParam(model, "emit", "N:man"), 1e-2);
        //        assertEquals(-4.79, getParam(model, "emit", "V:man"), 1e-2);
        //        assertEquals(-2.47, getParam(model, "emit", "N:jump"), 1e-2);
        //        assertEquals(2.47, getParam(model, "emit", "V:jump"), 1e-2);
        //        assertEquals(-3.82, getParam(model, "emit", "N:fence"), 1e-2);
        //        assertEquals(3.82, getParam(model, "emit", "V:fence"), 1e-2);
        //        
        //        assertEquals(-2.31, getParam(model, "tran", "N:N"), 1e-2);
        //        assertEquals(0.65, getParam(model, "tran", "N:V"), 1e-2);
        //        assertEquals(1.66, getParam(model, "tran", "V:V"), 1e-2);
    }

    @Test
    public void testTrainWithLatentVars() {
        FactorTemplateList fts = new FactorTemplateList();        
        ObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);
        
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVarsLatent(ofc, obsFe);

        VarConfig trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);

        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(new LabeledFgExample(fgv.fg, trainConfig, obsFe, fts));
        ofc.init(data);
        FgModel model = new FgModel(ofc.getNumParams());
        //model.setParams(new double[]{1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0});
        model = train(model, data);
        
        System.out.println(fts);
        System.out.println(DoubleArrays.toString(FgModelTest.getParams(model), "%.2f"));
        JUnitUtils.assertArrayEquals(new double[]{0.35, 0.35, -0.35, -0.35, 0.14, -0.14, 0.14, -0.14, -6.26, 11.09, -7.31, 2.48}, FgModelTest.getParams(model), 1e-2);
    }
    
    public enum MockTemplate {
        UNARY, ROLE_UNARY
    }
    
    @Test
    public void testTrainWithGlobalFactor() {
        FactorTemplateList fts = new FactorTemplateList();  
        ObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        
        final int n = 3;
        FactorGraph fg = new FactorGraph();
        ProjDepTreeFactor treeFac = new ProjDepTreeFactor(n, VarType.LATENT);
        LinkVar[] rootVars = treeFac.getRootVars();
        LinkVar[][] childVars = treeFac.getChildVars();
        Var[][] childRoles = new Var[n][n];
        
        // Add unary factors to each edge.
        VarConfig trainConfig = new VarConfig();

        for (int i=-1; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    ExpFamFactor f;
                    if (i == -1) {
                        f = new ObsFeExpFamFactor(new VarSet(rootVars[j]), MockTemplate.UNARY, ofc, obsFe);
                        fg.addFactor(f);

                        //trainConfig.put(rootVars[j], 0);
                    } else {
                        f = new ObsFeExpFamFactor(new VarSet(childVars[i][j]), MockTemplate.UNARY, ofc, obsFe);
                        fg.addFactor(f);

                        childRoles[i][j] = new Var(VarType.PREDICTED, 3, "Role"+i+"_"+j, QLists.getList("A1", "A2", "A3"));
                        fg.addFactor(new ObsFeExpFamFactor(new VarSet(childRoles[i][j]), MockTemplate.ROLE_UNARY, ofc, obsFe));
                        
                        //trainConfig.put(childVars[i][j], 0);
                        trainConfig.put(childRoles[i][j], "A1");
                    }
                }
            }
        }
        
        //trainConfig.put(rootVars[0], 1);
        //trainConfig.put(childVars[0][1], 1);
        trainConfig.put(childRoles[0][1], "A2");
        trainConfig.put(childRoles[1][0], "A2");   
        
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(new LabeledFgExample(fg, trainConfig, obsFe, fts));
        ofc.init(data);
        FgModel model = new FgModel(ofc.getNumParams());
        //model.setParams(new double[]{1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0});
        model = train(model, data);
        
        System.out.println(fts);
        System.out.println(DoubleArrays.toString(FgModelTest.getParams(model), "%.2f"));
        // [FALSE, TRUE, A1, A2, A3]
        JUnitUtils.assertArrayEquals(new double[]{0.00, 0.00, 2.60, 1.90, -4.51}, FgModelTest.getParams(model), 1e-2);

    }
    
    public static FgModel train(FgModel model, FgExampleList data) {
        return train(model, data, null, false);
    }
    
    public static FgModel train(FgModel model, FgExampleList data, Regularizer r, boolean sgd) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.s = LogSemiring.getInstance();
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        
        if (sgd) {
            // Run with SGD
            SGDPrm optPrm = new SGDPrm();
            optPrm.numPasses = 10;
            optPrm.batchSize = 2;
            optPrm.autoSelectLr = false;
            optPrm.sched.setEta0(0.1);
            prm.batchOptimizer = new SGD(optPrm);
            prm.optimizer = null;
        } else {
            prm.batchOptimizer = null;
            prm.optimizer = new MalletLBFGS(new MalletLBFGSPrm());
        }
        prm.regularizer = r;
        
        CrfTrainer trainer = new CrfTrainer(prm);
        trainer.train(model, data);
        return model;
    }
    
    public static FgModel trainErma(FgModel model, FgExampleList data, Regularizer r, boolean sgd) {
        ErmaBpPrm bpPrm = new ErmaBpPrm();
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.s = RealAlgebra.getInstance();
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        prm.bFactory = bpPrm;
        prm.dlFactory = new MeanSquaredErrorFactory();
        //prm.dlFactory = new ExpectedRecallFactory();
        prm.trainer = Trainer.ERMA;
             
        if (sgd) {
            // Run with SGD
            SGDPrm optPrm = new SGDPrm();
            optPrm.numPasses = 10;
            optPrm.batchSize = 2;
            optPrm.autoSelectLr = false;
            optPrm.sched.setEta0(0.2);
            prm.batchOptimizer = new SGD(optPrm);
            prm.optimizer = null;
        } else {
            prm.batchOptimizer = null;
            prm.optimizer = new MalletLBFGS(new MalletLBFGSPrm());
        }
        prm.regularizer = r;
        
        CrfTrainer trainer = new CrfTrainer(prm);
        trainer.train(model, data);
        return model;
    }
}
