package edu.jhu.gm;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfObjectiveTest.LogLinearEDs;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.FactorGraphTest.FgAndVars;
import edu.jhu.gm.data.ErmaReader;
import edu.jhu.gm.data.ErmaReaderTest;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;

public class CrfTrainerTest {

    /**
     * Constructs features for each factor graph configuration by creating a
     * sorted list of all the variable states and concatenating them together.
     * 
     * @author mgormley
     */
    public static class SimpleVCFeatureExtractor implements FeatureExtractor {

        private Alphabet<Feature> alphabet;
        
        public SimpleVCFeatureExtractor(Alphabet<Feature> alphabet) {
            this.alphabet = alphabet;
        }
        
        // Just concatenates all the state names together (in-order).
        @Override
        public FeatureVector calcFeatureVector(int factorId, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();
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
        FgModel model = new FgModel(exs.getAlphabet());
        model.setParams(params);
        
        model = train(model, exs.getData());
        
        JUnitUtils.assertArrayEquals(new double[]{1.098, 0.693}, model.getParams(), 1e-3);
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

        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FeatureExtractor featExtractor = new SimpleVCFeatureExtractor(alphabet);
        
        FgExamples data = new FgExamples(alphabet);
        data.add(new FgExample(fgv.fg, trainConfig, featExtractor));
        FgModel model = new FgModel(alphabet);

        model = train(model, data);
        
        System.out.println(Utilities.toString(model.getParams(), "%.2f"));
        JUnitUtils.assertArrayEquals(new double[]{4.79, -4.79, 2.47, -2.47, 3.82, -3.82, 0.65, -2.31, 1.66}, model.getParams(), 1e-2);
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

        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FeatureExtractor featExtractor = new SimpleVCFeatureExtractor(alphabet);
        
        FgExamples data = new FgExamples(alphabet);
        data.add(new FgExample(fgv.fg, trainConfig, featExtractor));
        FgModel model = new FgModel(alphabet);
        //model.setParams(new double[]{1, 2, 3, 4, 5, 6, 0, 0, 0, 0, 0, 0, 0});
        model = train(model, data);
        
        System.out.println(alphabet.getObjects());
        System.out.println(Utilities.toString(model.getParams(), "%.2f"));
        JUnitUtils.assertArrayEquals(new double[]{-0.00, -0.00, -0.00, -0.00, 0.00, 0.00, 3.45, 3.45, -3.45, -3.45, 1.64, -10.18, 8.54}, model.getParams(), 1e-2);
    }

    @Test
    public void testTrainErmaInput() {
        ErmaReader er = new ErmaReader(true);
        er.read(ErmaReaderTest.ERMA_TOY_FEATURE_FILE, ErmaReaderTest.ERMA_TOY_TRAIN_DATA_FILE);
        FgExamples data = er.getDataExs();
        Alphabet<Feature> alphabet = data.getAlphabet();
        
        FgModel model = new FgModel(alphabet);
        model = train(model, data);
        
        // ERMA achieves the following log-likelihood: 0.5802548014360731.
        // Our CRF obtains LL: -0.0013527881300134936.
        
        // Note: This doesn't test the result, just that nothing throws an exception.
    }
    
    private FgModel train(FgModel model, FgExamples data) {
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
        return trainer.train(model, data);
    }

}
