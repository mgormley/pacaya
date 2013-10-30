package edu.jhu.gm.decode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureTemplateList;
import edu.jhu.gm.inf.BeliefPropagationTest;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.train.CrfTrainerTest;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor;
import edu.jhu.util.Alphabet;

public class MbrDecoderTest {

    @Test
    public void testAccuracy() {
        FactorGraph fg = BeliefPropagationTest.getThreeConnectedComponentsFactorGraph(true);
        MbrDecoderPrm prm = new MbrDecoderPrm();
        MbrDecoder decoder = new MbrDecoder(prm);
        
        // Make a dummy train config.
        VarConfig trainConfig = new VarConfig();
        for (Var var : fg.getVars()) {
            trainConfig.put(var, 0);
        }
        
        FeatureTemplateList fts = new FeatureTemplateList();
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        data.add(new FgExample(fg, trainConfig, new SimpleVCFeatureExtractor(fg, trainConfig, fts), fts));
        FgModel model = new FgModel(fts);

        fts.stopGrowth();
        
        // Set the param for "N" to 0.5.
        model.add(0, 0, fts.get(0).getAlphabet().lookupIndex(new Feature("BIAS_FEATURE")), 0.5);
        //model.getParams()[0] = 0.5;
        // Set the param for "V" to 1.0.
        model.add(0, 1, fts.get(0).getAlphabet().lookupIndex(new Feature("BIAS_FEATURE")), 1.0);
        //model.getParams()[1] = 1.0;
        System.out.println(model);
        
        decoder.decode(model, data.get(0));
        
        VarConfig mbrVc = decoder.getMbrVarConfig();
        
        assertEquals("V", mbrVc.getStateName(fg.getVars().get(0)));
        System.out.println(mbrVc);
    }

}
