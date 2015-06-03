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
import edu.jhu.pacaya.autodiff.erma.ErmaObjectiveTest;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.autodiff.erma.MeanSquaredError.MeanSquaredErrorFactory;
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
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphTest;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.FgModelTest;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.pacaya.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.pacaya.gm.train.CrfTrainer.Trainer;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.collections.Lists;
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
        fvs = new List[]{ Lists.getList("x=A,y=A"), Lists.getList("x=A,y=B") };
        xyData.addExStrFeats(1.0, "x=A", "y=A", fvs);
        fvs = new List[]{ Lists.getList("x=B,y=A"), Lists.getList("x=B,y=B") };
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
    public void testTrainNoLatentVars() {
        // Boiler plate feature extraction code.
        FactorTemplateList fts = new FactorTemplateList();        
        ObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);

        // Create the factor graph.
        FgAndVars fgv = getLinearChainFgWithVars(ofc, obsFe);

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
        model = train(model, data);
        
        // Assertions:
        System.out.println(model);
        System.out.println(fts);
        System.out.println(DoubleArrays.toString(FgModelTest.getParams(model), "%.2f"));
        JUnitUtils.assertArrayEquals(new double[]{3.32, -5.43, 1.05, 1.05, -7.09, -7.26, 10.45, 3.89}, FgModelTest.getParams(model), 1e-2);
        
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
        
        FgAndVars fgv = getLinearChainFgWithVarsLatent(ofc, obsFe);

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
        JUnitUtils.assertArrayEquals(new double[]{0.35, -0.35, 0.35, -0.35, 0.14, 0.14, -0.14, -0.14, -6.26, -7.31, 11.09, 2.48}, FgModelTest.getParams(model), 1e-2);
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

                        childRoles[i][j] = new Var(VarType.PREDICTED, 3, "Role"+i+"_"+j, Lists.getList("A1", "A2", "A3"));
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
        bpPrm.s = LogSemiring.LOG_SEMIRING;
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
        bpPrm.s = RealAlgebra.REAL_ALGEBRA;
        
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

    /**
     * This method differs from {@link FactorGraphTest}'s version in that it uses a feature extractor.
     */
    public static FgAndVars getLinearChainFgWithVars(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", Lists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", Lists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", Lists.getList("fence", "bucket"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Lists.getList("N", "V"));

        // Emission factors. 
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(t0, w0), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit1 = new ObsFeExpFamFactor(new VarSet(t1, w1), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit2 = new ObsFeExpFamFactor(new VarSet(t2, w2), "emit", ofc, obsFe); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Transition factors.
        ObsFeExpFamFactor tran0 = new ObsFeExpFamFactor(new VarSet(t0, t1), "tran", ofc, obsFe); 
        ObsFeExpFamFactor tran1 = new ObsFeExpFamFactor(new VarSet(t1, t2), "tran", ofc, obsFe); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        for (Factor f : fg.getFactors()) {
            ((ExpFamFactor)f).convertRealToLog();
        }

        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
        return fgv;
    }
    
    public static FgAndVars getLinearChainFgWithVarsLatent(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.PREDICTED, 2, "w0", Lists.getList("man", "dog"));
        Var w1 = new Var(VarType.PREDICTED, 2, "w1", Lists.getList("run", "jump"));
        Var w2 = new Var(VarType.PREDICTED, 2, "w2", Lists.getList("fence", "bucket"));

        // Create latent classes.
        Var z0 = new Var(VarType.LATENT, 2, "z0", Lists.getList("C1", "C2"));
        Var z1 = new Var(VarType.LATENT, 2, "z1", Lists.getList("C1", "C2"));
        Var z2 = new Var(VarType.LATENT, 2, "z2", Lists.getList("C1", "C2"));
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Lists.getList("N", "V"));

        // Emission factors. 
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(z0, w0), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit1 = new ObsFeExpFamFactor(new VarSet(z1, w1), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit2 = new ObsFeExpFamFactor(new VarSet(z2, w2), "emit", ofc, obsFe); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 0.3);
        emit1.setValue(1, 0.7);
        emit2.setValue(0, 0.5);
        emit2.setValue(1, 0.5);
        
        // Latent emission factors. 
        ObsFeExpFamFactor emitL0 = new ObsFeExpFamFactor(new VarSet(t0, z0), "latent-emit", ofc, obsFe); 
        ObsFeExpFamFactor emitL1 = new ObsFeExpFamFactor(new VarSet(t1, z1), "latent-emit", ofc, obsFe); 
        ObsFeExpFamFactor emitL2 = new ObsFeExpFamFactor(new VarSet(t2, z2), "latent-emit", ofc, obsFe); 

        emitL0.setValue(0, 1.1);
        emitL0.setValue(1, 1.9);
        emitL1.setValue(0, 1.3);
        emitL1.setValue(1, 1.7);
        emitL2.setValue(0, 1.5);
        emitL2.setValue(1, 1.5);
        
        // Transition factors.
        ObsFeExpFamFactor tran0 = new ObsFeExpFamFactor(new VarSet(t0, t1), "tran", ofc, obsFe); 
        ObsFeExpFamFactor tran1 = new ObsFeExpFamFactor(new VarSet(t1, t2), "tran", ofc, obsFe); 
        
        tran0.fill(1);
        tran0.setValue(0, 0.2);
        tran0.setValue(1, 0.3);
        tran0.setValue(2, 0.4);
        tran0.setValue(3, 0.5);
        tran1.fill(1);
        tran1.setValue(0, 1.2);
        tran1.setValue(1, 1.3);
        tran1.setValue(2, 1.4);
        tran1.setValue(3, 1.5);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(emitL0);
        fg.addFactor(emitL1);
        fg.addFactor(emitL2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);

        for (Factor f : fg.getFactors()) {
            ((ExpFamFactor)f).convertRealToLog();
        }
        
        FgAndVars fgv = new FgAndVars();
        fgv.fg = fg;
        fgv.w0 = w0;
        fgv.w1 = w1;
        fgv.w2 = w2;
        fgv.z0 = z0;
        fgv.z1 = z1;
        fgv.z2 = z2;
        fgv.t0 = t0;
        fgv.t1 = t1;
        fgv.t2 = t2;
        return fgv;
    }
}
