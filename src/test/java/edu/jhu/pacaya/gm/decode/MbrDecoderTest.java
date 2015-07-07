package edu.jhu.pacaya.gm.decode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.ObsFeExpFamFactor;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner;
import edu.jhu.pacaya.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.pacaya.gm.feat.ObsFeatureExtractor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.train.SimpleVCObsFeatureExtractor;
import edu.jhu.pacaya.util.collections.QLists;

public class MbrDecoderTest {

    @Test
    public void testAccuracyLogDomain() {
    	testAccuracy(true);
    }
    
    @Test
    public void testAccuracyProbDomain() {
    	testAccuracy(false);
    }
    
    public void testAccuracy(boolean useLogDomain) {
        FactorTemplateList fts = new FactorTemplateList();        
        ObsFeatureExtractor obsFe = new SimpleVCObsFeatureExtractor(fts);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = true;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);
        
        FactorGraph fg = getThreeConnectedComponentsFactorGraph(ofc, obsFe);
        MbrDecoder decoder = new MbrDecoder(new MbrDecoderPrm());
        
        // Make a dummy train config.
        VarConfig trainConfig = new VarConfig();
        for (Var var : fg.getVars()) {
            trainConfig.put(var, 0);
        }
        
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(new LabeledFgExample(fg, trainConfig, obsFe, fts));
        ofc.init(data);
        FgModel model = new FgModel(ofc.getNumParams());

        fts.stopGrowth();
        
        // Set the param for "N" to 0.5.
        int feat;
        feat = ofc.getFeatIndex(0, 0, fts.get(0).getAlphabet().lookupIndex("BIAS_FEATURE"));
        model.add(feat, 0.5);
        //model.getParams()[0] = 0.5;
        // Set the param for "V" to 1.0.
        feat = ofc.getFeatIndex(0, 1, fts.get(0).getAlphabet().lookupIndex("BIAS_FEATURE"));
        model.add(feat, 1.0);
        //model.getParams()[1] = 1.0;
        System.out.println(model);
        
        decoder.decode(model, data.get(0));
        
        VarConfig mbrVc = decoder.getMbrVarConfig();
        
        assertEquals("V", mbrVc.getStateName(fg.getVars().get(0)));
        System.out.println(mbrVc);
    }

    public static FactorGraph getThreeConnectedComponentsFactorGraph(ObsFeatureConjoiner ofc, ObsFeatureExtractor obsFe) {
        FactorGraph fg = new FactorGraph();
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", QLists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", QLists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", QLists.getList("N", "V"));
        
        // Emission factors. 
        ObsFeExpFamFactor emit0 = new ObsFeExpFamFactor(new VarSet(t0), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit1 = new ObsFeExpFamFactor(new VarSet(t1), "emit", ofc, obsFe); 
        ObsFeExpFamFactor emit2 = new ObsFeExpFamFactor(new VarSet(t2), "emit", ofc, obsFe); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 3);
        emit1.setValue(1, 7);
        emit2.setValue(0, 1);
        emit2.setValue(1, 1);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);

        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }

        return fg;
    }

}
