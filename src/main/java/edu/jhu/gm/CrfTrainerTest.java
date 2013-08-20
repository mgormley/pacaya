package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfObjectiveTest.LogLinearEDs;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.FactorGraphTest.FgAndVars;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.data.ErmaReader;
import edu.jhu.gm.data.ErmaReaderTest;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;

public class CrfTrainerTest {

    /**
     * Constructs features for each factor graph configuration by creating a
     * sorted list of all the variable states and concatenating them together.
     * 
     * For testing only.
     * 
     * @author mgormley
     */
    public static class SimpleVCFeatureExtractor extends SlowObsFeatureExtractor {

        private FeatureTemplateList fts;

        public SimpleVCFeatureExtractor(FactorGraph fg, VarConfig goldConfig, FeatureTemplateList fts) {
            super(fg, goldConfig);
            this.fts = fts;
        }
        
        // Just concatenates all the state names together (in-order).
        @Override
        public FeatureVector calcObsFeatureVector(int factorId, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();
            Alphabet<Feature> alphabet = fts.getTemplate(fg.getFactor(factorId)).getAlphabet();

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
            
            int featIdx = alphabet.lookupIndex(new Feature("BIAS_FEATURE"));
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

        FgExamples data = exs.getData();
        double[] params = new double[]{3.0, 2.0};
        FgModel model = new FgModel(data.getTemplates());
        model.updateModelFromDoubles(params);
        
        model = train(model, exs.getData());
        
        JUnitUtils.assertArrayEquals(new double[]{1.098, 0.693}, FgModelTest.getParams(model), 1e-3);
    }
    
    @Test
    public void testTrainNoLatentVars() {
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(true);

        VarConfig trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);

        FeatureTemplateList fts = new FeatureTemplateList();        
        ObsFeatureExtractor featExtractor = new SimpleVCFeatureExtractor(fgv.fg, trainConfig, fts);
        
        FgExamples data = new FgExamples(fts);
        data.add(new FgExample(fgv.fg, trainConfig, featExtractor, fts));
        FgModel model = new FgModel(fts);

        model = train(model, data);
        
        System.out.println(model);

        assertEquals(4.79, getParam(model, "emit", "N:man"), 1e-2);
        assertEquals(-4.79, getParam(model, "emit", "V:man"), 1e-2);
        assertEquals(-2.47, getParam(model, "emit", "N:jump"), 1e-2);
        assertEquals(2.47, getParam(model, "emit", "V:jump"), 1e-2);
        assertEquals(-3.82, getParam(model, "emit", "N:fence"), 1e-2);
        assertEquals(3.82, getParam(model, "emit", "V:fence"), 1e-2);
        
        assertEquals(-2.31, getParam(model, "tran", "N:N"), 1e-2);
        assertEquals(0.65, getParam(model, "tran", "N:V"), 1e-2);
        assertEquals(1.66, getParam(model, "tran", "V:V"), 1e-2);
    }
    
    private double getParam(FgModel model, Object templateKey, String name) {
        FeatureTemplate ft = model.getTemplates().getTemplateByKey(templateKey);
        int feat = ft.getAlphabet().lookupIndex(new Feature(name));
        throw new RuntimeException("Somehow we need access to the configId if we want to use this method.");
        //return model.get
        //return model.getParams()[model.getAlphabet().lookupIndex(new Feature(name))];
    }

    @Test
    public void testTrainWithLatentVars() {
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVarsLatent(true);

        VarConfig trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);

        FeatureTemplateList fts = new FeatureTemplateList();        
        ObsFeatureExtractor featExtractor = new SimpleVCFeatureExtractor(fgv.fg, trainConfig, fts);
        
        FgExamples data = new FgExamples(fts);
        data.add(new FgExample(fgv.fg, trainConfig, featExtractor, fts));
        FgModel model = new FgModel(fts);
        //model.setParams(new double[]{1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0});
        model = train(model, data);
        
        System.out.println(fts);
        System.out.println(Utilities.toString(FgModelTest.getParams(model), "%.2f"));
        //[C1:man, C2:man, C1:jump, C2:jump, C1:fence, C2:fence, C1:N, C2:N, C1:V, C2:V, N:N, N:V, V:V]
        JUnitUtils.assertArrayEquals(new double[]{-0.00, -0.00, -0.00, -0.00, 0.00, 0.00, 3.45, 3.45, -3.45, -3.45, -10.18, 1.64, 8.54}, FgModelTest.getParams(model), 1e-2);
    }

    @Test
    public void testTrainWithGlobalFactor() {
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
                        f = new ExpFamFactor(new VarSet(rootVars[j]), SrlFactorTemplate.LINK_UNARY);
                        fg.addFactor(f);

                        //trainConfig.put(rootVars[j], 0);
                    } else {
                        f = new ExpFamFactor(new VarSet(childVars[i][j]), SrlFactorTemplate.LINK_UNARY);
                        fg.addFactor(f);

                        childRoles[i][j] = new Var(VarType.PREDICTED, 3, "Role"+i+"_"+j, Utilities.getList("A1", "A2", "A3"));
                        fg.addFactor(new ExpFamFactor(new VarSet(childRoles[i][j]), SrlFactorTemplate.ROLE_UNARY));
                        
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

        FeatureTemplateList fts = new FeatureTemplateList();        
        ObsFeatureExtractor featExtractor = new SimpleVCFeatureExtractor(fg, trainConfig, fts);
        
        FgExamples data = new FgExamples(fts);
        data.add(new FgExample(fg, trainConfig, featExtractor, fts));
        FgModel model = new FgModel(fts);
        //model.setParams(new double[]{1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0});
        model = train(model, data);
        
        System.out.println(fts);
        System.out.println(Utilities.toString(FgModelTest.getParams(model), "%.2f"));
        // [FALSE, TRUE, A1, A2, A3]
        JUnitUtils.assertArrayEquals(new double[]{0.00, 0.00, 2.60, 1.90, -4.51}, FgModelTest.getParams(model), 1e-2);

    }
    
    @Test
    public void testTrainErmaInput() {
        ErmaReader er = new ErmaReader(true);
        FeatureTemplateList fts = new FeatureTemplateList();        
        FgExamples data = er.read(ErmaReaderTest.ERMA_TOY_FEATURE_FILE, ErmaReaderTest.ERMA_TOY_TRAIN_DATA_FILE, fts);
        
        FgModel model = new FgModel(fts);
        model = train(model, data);
        
        // ERMA achieves the following log-likelihood: 0.5802548014360731.
        // Our CRF obtains LL: -0.0013527881300134936.
        
        // Note: This doesn't test the result, just that nothing throws an exception.
    }
    
    private static FgModel train(FgModel model, FgExamples data) {
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

}
