package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.CrfTrainerTest.SimpleVCFeatureExtractor;
import edu.jhu.gm.MbrDecoder.MbrDecoderPrm;
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
        
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        FgExamples data = new FgExamples(alphabet);
        data.add(new FgExample(fg, trainConfig, new SimpleVCFeatureExtractor(fg, trainConfig, alphabet)));
        FgModel model = new FgModel(alphabet);

        // Set the param for "N" to 0.5.
        model.getParams()[0] = 0.5;
        // Set the param for "V" to 1.0.
        model.getParams()[1] = 1.0;
        System.out.println(model);
        
        decoder.decode(model, data);
        
        VarConfig mbrVc = decoder.getMbrVarConfig(0);
        
        assertEquals("V", mbrVc.getStateName(fg.getVars().get(0)));
        System.out.println(mbrVc);
    }

}
