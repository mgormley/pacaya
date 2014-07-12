package edu.jhu.gm.train;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.erma.ErmaReader;
import edu.jhu.gm.data.erma.ErmaReaderTest;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.feat.SlowFeatureExtractor;
import edu.jhu.gm.feat.SlowObsFeatureExtractor;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.maxent.LogLinearEDs;
import edu.jhu.gm.maxent.LogLinearXY;
import edu.jhu.gm.maxent.LogLinearXY.LogLinearXYPrm;
import edu.jhu.gm.maxent.LogLinearXYData;
import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.FgModelTest;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.srl.DepParseFactorGraphBuilder.DepParseFactorTemplate;
import edu.jhu.srl.SrlFactorGraphBuilder.SrlFactorTemplate;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.collections.Lists;

public class CrfTrainerTest {

    /**
     * Constructs features for each factor graph configuration by creating a
     * sorted list of all the variable states and concatenating them together.
     * 
     * For testing only.
     * 
     * @author mgormley
     */
    public static class SimpleVCFeatureExtractor2 extends SlowFeatureExtractor {

        private Alphabet<Feature> alphabet;

        public SimpleVCFeatureExtractor2(Alphabet<Feature> alphabet) {
            super();
            this.alphabet = alphabet;          
        }
        
        // Just concatenates all the state names together (in-order).
        @Override
        public FeatureVector calcFeatureVector(FeExpFamFactor factor, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();

            if (varConfig.size() > 0) {
                String[] strs = new String[varConfig.getVars().size()];
                int i=0;
                for (Var v : varConfig.getVars()) {
                    strs[i] = varConfig.getStateName(v);
                    i++;
                }
                Arrays.sort(strs);
                Feature feat = new Feature(StringUtils.join(strs, ":"));
                int featIdx = alphabet.lookupIndex(feat);
                fv.set(featIdx, 1.0);
            }
            
            int featIdx = alphabet.lookupIndex(new Feature("BIAS_FEATURE", true));
            fv.set(featIdx, 1.0);
            
            return fv;
        }
    }
    
    /**
     * Constructs features for each factor graph configuration by creating a
     * sorted list of all the variable states and concatenating them together.
     * 
     * For testing only.
     * 
     * @author mgormley
     */
    public static class SimpleVCFeatureExtractor extends SlowObsFeatureExtractor {

        private FactorTemplateList fts;

        public SimpleVCFeatureExtractor(FactorTemplateList fts) {
            super();
            this.fts = fts;
        }
        
        // Just concatenates all the state names together (in-order).
        @Override
        public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();
            Alphabet<Feature> alphabet = fts.getTemplate(factor).getAlphabet();

            if (varConfig.size() > 0) {
                String[] strs = new String[varConfig.getVars().size()];
                int i=0;
                for (Var v : varConfig.getVars()) {
                    strs[i] = varConfig.getStateName(v);
                    i++;
                }
                Arrays.sort(strs);
                Feature feat = new Feature(StringUtils.join(strs, ":"));
                int featIdx = alphabet.lookupIndex(feat);
                fv.set(featIdx, 1.0);
            }
            
            int featIdx = alphabet.lookupIndex(new Feature("BIAS_FEATURE", true));
            fv.set(featIdx, 1.0);
            
            return fv;
        }
    }

    @Test 
    public void testLogLinearModelShapes() {
        LogLinearEDs exs = new LogLinearEDs();
        exs.addEx(30, "circle", "solid");
        exs.addEx(15, "circle");
        exs.addEx(10, "solid");
        exs.addEx(5);

        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(params.length);
        model.updateModelFromDoubles(params);
        
        LogLinearXYData data = exs.getData();
        LogLinearXY maxent = new LogLinearXY(new LogLinearXYPrm());
        
        model = train(model, maxent.getData(data));
        
        // Note: this used to be 1.093.
        JUnitUtils.assertArrayEquals(new double[]{1.098, 0.693}, FgModelTest.getParams(model), 1e-3);
    }
    
    @Test
    public void testTrainNoLatentVars() {
        // Boiler plate feature extraction code.
        FactorTemplateList fts = new FactorTemplateList();        
        ObsFeatureExtractor obsFe = new SimpleVCFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);

        // Create the factor graph.
        FgAndVars fgv = getLinearChainFgWithVars(true, ofc, obsFe);

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
        //FeatureTemplateList [isGrowing=true, fts=[FeatureTemplate [key=emit, numConfigs=2, alphabet=Alphabet [idxObjMap=[man, BIAS_FEATURE, jump, fence], isGrowing=true]], FeatureTemplate [key=tran, numConfigs=4, alphabet=Alphabet [idxObjMap=[BIAS_FEATURE], isGrowing=true]]]]
        JUnitUtils.assertArrayEquals(new double[]{-0.10, -0.10, 0.10, 0.10, -3.15, -3.15, -3.29, -3.29, 5.30, 5.30, 1.14, 1.14}, FgModelTest.getParams(model), 1e-2);
        
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
        ObsFeatureExtractor obsFe = new SimpleVCFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);
        
        FgAndVars fgv = getLinearChainFgWithVarsLatent(true, ofc, obsFe);

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
        //FeatureTemplateList [isGrowing=true, fts=[FeatureTemplate [key=emit, numConfigs=2, alphabet=Alphabet [idxObjMap=[man, BIAS_FEATURE, jump, fence], isGrowing=true]], FeatureTemplate [key=latent-emit, numConfigs=4, alphabet=Alphabet [idxObjMap=[BIAS_FEATURE], isGrowing=true]], FeatureTemplate [key=tran, numConfigs=4, alphabet=Alphabet [idxObjMap=[BIAS_FEATURE], isGrowing=true]]]]
        JUnitUtils.assertArrayEquals(new double[]{-0.00, -0.00, -0.00, -0.00, 0.01, 0.01, 0.01, 0.01, -0.01, -0.01, -0.01, -0.01, -3.08, -3.08, -3.33, -3.33, 5.25, 5.25, 1.16, 1.16}, FgModelTest.getParams(model), 1e-2);
          
        // OLD PARAMS:
        //[C1:man, C2:man, C1:jump, C2:jump, C1:fence, C2:fence, C1:N, C2:N, C1:V, C2:V, N:N, N:V, V:V]
        //JUnitUtils.assertArrayEquals(new double[]{-0.00, -0.00, -0.00, -0.00, 0.00, 0.00, 3.45, 3.45, -3.45, -3.45, -10.18, 1.64, 8.54}, FgModelTest.getParams(model), 1e-2);
    }

    @Test
    public void testTrainWithGlobalFactor() {
        FactorTemplateList fts = new FactorTemplateList();  
        ObsFeatureExtractor obsFe = new SimpleVCFeatureExtractor(fts);
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
                        f = new ObsFeExpFamFactor(new VarSet(rootVars[j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe);
                        fg.addFactor(f);

                        //trainConfig.put(rootVars[j], 0);
                    } else {
                        f = new ObsFeExpFamFactor(new VarSet(childVars[i][j]), DepParseFactorTemplate.LINK_UNARY, ofc, obsFe);
                        fg.addFactor(f);

                        childRoles[i][j] = new Var(VarType.PREDICTED, 3, "Role"+i+"_"+j, Lists.getList("A1", "A2", "A3"));
                        fg.addFactor(new ObsFeExpFamFactor(new VarSet(childRoles[i][j]), SrlFactorTemplate.ROLE_UNARY, ofc, obsFe));
                        
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
    
    // This is slow and relies on hard-coded paths.
    //TODO: @Test
    public void testTrainErmaInput() {
        ErmaReader er = new ErmaReader();
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FgExampleList data = er.read(ErmaReaderTest.ERMA_TOY_FEATURE_FILE, ErmaReaderTest.ERMA_TOY_TRAIN_DATA_FILE, alphabet);
        
        FgModel model = new FgModel(alphabet.size());
        model = train(model, data);
        
        // ERMA achieves the following log-likelihood: 0.5802548014360731.
        // Our CRF obtains LL: -0.0013527881300134936.
        
        // Note: This doesn't test the result, just that nothing throws an exception.
    }
    
    private static FgModel train(FgModel model, FgExampleList data) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = true;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        
        // To run with SGD, uncomment these lines.
        //        SGDPrm optPrm = new SGDPrm();
        //        optPrm.iterations = 100;
        //        optPrm.lrAtMidpoint = 0.1;
        //        prm.maximizer = new SGD(optPrm);
        prm.regularizer = null;
        
        CrfTrainer trainer = new CrfTrainer(prm);
        trainer.train(model, data);
        return model;
    }

    public static FgAndVars getLinearChainFgWithVars(boolean logDomain, ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", Lists.getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", Lists.getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", Lists.getList("fence", "bucket"));
        
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
        
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((ExpFamFactor)f).convertRealToLog();
            }
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
    
    public static FgAndVars getLinearChainFgWithVarsLatent(boolean logDomain, ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {

        FactorGraph fg = new FactorGraph();

        // Create three words.
        Var w0 = new Var(VarType.OBSERVED, 2, "w0", Lists.getList("man", "dog"));
        Var w1 = new Var(VarType.OBSERVED, 2, "w1", Lists.getList("run", "jump"));
        Var w2 = new Var(VarType.OBSERVED, 2, "w2", Lists.getList("fence", "bucket"));

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

        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((ExpFamFactor)f).convertRealToLog();
            }
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
