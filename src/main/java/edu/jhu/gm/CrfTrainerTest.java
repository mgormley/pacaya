package edu.jhu.gm;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import edu.jhu.gm.FactorGraphTest.FgAndVars;
import edu.jhu.optimize.SGD;
import edu.jhu.optimize.SGD.SGDPrm;
import edu.jhu.util.Alphabet;

public class CrfTrainerTest {

    public static class MockFeatureExtractor implements FeatureExtractor {

        private FactorGraph fg;
        private Alphabet<Feature> alphabet;
        
        public MockFeatureExtractor(FactorGraph fg, Alphabet<Feature> alphabet) {
            this.fg = fg;
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
    public void test() {
        SGDPrm sgdPrm = new SGDPrm();
        SGD maximizer = new SGD(sgdPrm);
        
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars();

        // TODO: add a latent var.
        // Var l0 = new Var(VarType.LATENT, 3, "l0", getList("TOPIC1", "TOPIC2", "TOPIC3"));
        
        VarConfig trainConfig = new VarConfig();
        trainConfig.put(fgv.w0, 0);
        trainConfig.put(fgv.w1, 1);
        trainConfig.put(fgv.w2, 0);
        trainConfig.put(fgv.t0, 0);
        trainConfig.put(fgv.t1, 1);
        trainConfig.put(fgv.t2, 1);

        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FeatureExtractor featExtractor = new MockFeatureExtractor(fgv.fg, alphabet);
        
        FgExamples data = new FgExamples();
        data.add(new FgExample(fgv.fg, trainConfig, featExtractor));
        FgModel model = new FgModel(alphabet);
        
        CrfTrainer trainer = new CrfTrainer(model, data, maximizer);
        trainer.train();
        
        fail("Not yet implemented");
    }

}
