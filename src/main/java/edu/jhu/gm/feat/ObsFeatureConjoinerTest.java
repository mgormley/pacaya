package edu.jhu.gm.feat;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.collections.Lists;

public class ObsFeatureConjoinerTest {

    @Test
    public void testNumParams() {
        FactorTemplateList fts = getFtl();
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        ofc.init();
        assertEquals((3*2)*2 + 2*1, ofc.getNumParams());
    }

    @Test
    public void testExcludeUnsupportedFeatures1() {
        boolean useLat = false;
        boolean includeUnsupportedFeatures = true;
        checkNumParams(useLat, includeUnsupportedFeatures, 20);        
    }
    
    @Test
    public void testExcludeUnsupportedFeatures2() {
        boolean useLat = false;
        boolean includeUnsupportedFeatures = false;
        // 6 bias features, and 4 other features.
        checkNumParams(useLat, includeUnsupportedFeatures, 6+4);
    }
    
    @Test
    public void testExcludeUnsupportedFeaturesWithLatentVars1() {
        boolean useLat = true;
        boolean includeUnsupportedFeatures = true;
        checkNumParams(useLat, includeUnsupportedFeatures, 20);        
    }
    
    @Test
    public void testExcludeUnsupportedFeaturesWithLatentVars2() {
        boolean useLat = true;
        boolean includeUnsupportedFeatures = false;
        // 6 bias features, and 6 other features.
        checkNumParams(useLat, includeUnsupportedFeatures, 6+6);
    }

    private void checkNumParams(boolean useLat, boolean includeUnsupportedFeatures,
            int expectedNumParams) {
        FactorTemplateList fts = getFtl(useLat);
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.includeUnsupportedFeatures = includeUnsupportedFeatures;
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(prm, fts);
        
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(getExForFts("1a", "2a", ofc, fts, useLat));
        data.add(getExForFts("1a", "2c", ofc, fts, useLat));
        data.add(getExForFts("1b", "2b", ofc, fts, useLat));
        data.add(getExForFts("1b", "2c", ofc, fts, useLat));

        ofc.init(data);
        
        System.out.println("\n"+ofc);
        assertEquals(expectedNumParams, ofc.getNumParams());
    }
    
    public static class MockFeatureExtractor extends SlowObsFeatureExtractor {

        public MockFeatureExtractor() {
            super();
        }
        
        @Override
        public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor, VarConfig varConfig) {
            FeatureVector fv = new FeatureVector();
            FeatureNames alphabet = fts.getTemplate(factor).getAlphabet();

            int featIdx = alphabet.lookupIndex(new Feature("BIAS_FEATURE", true));
            fv.set(featIdx, 1.0);
            featIdx = alphabet.lookupIndex(new Feature("feat2a"));
            fv.set(featIdx, 1.0);
            
            return fv;
        }
    }
    
    private LFgExample getExForFts(String state1, String state2, ObsFeatureConjoiner ofc, FactorTemplateList fts, boolean useLat) {
        Var v1 = new Var(VarType.PREDICTED, 2, "1", Lists.getList("1a", "1b"));
        Var v2 = new Var(useLat ? VarType.LATENT : VarType.PREDICTED, 3, "2", Lists.getList("2a", "2b", "2c"));
        FactorGraph fg = new FactorGraph();
        MockFeatureExtractor obsFe = new MockFeatureExtractor();
        fg.addFactor(new ObsFeExpFamFactor(new VarSet(v1, v2), "key2", ofc, obsFe));
        
        VarConfig vc = new VarConfig();
        vc.put(v1, state1);
        vc.put(v2, state2);
        
        return new LabeledFgExample(fg, vc, obsFe, fts);
    }

    public static FactorTemplateList getFtl() {
        return getFtl(false);
    }
    
    public static FactorTemplateList getFtl(boolean useLat) {
        FactorTemplateList fts = new FactorTemplateList();
        Var v1 = new Var(VarType.PREDICTED, 2, "1", Lists.getList("1a", "1b"));
        Var v2 = new Var(useLat ? VarType.LATENT : VarType.PREDICTED, 3, "2", Lists.getList("2a", "2b", "2c"));
        {
            FeatureNames alphabet = new FeatureNames();
            alphabet.lookupIndex(new Feature("feat1"));
            fts.add(new FactorTemplate(new VarSet(v1), alphabet, "key1"));
        }
        {
            FeatureNames alphabet = new FeatureNames();
            alphabet.lookupIndex(new Feature("feat2a"));
            alphabet.lookupIndex(new Feature("feat2b"));
            fts.add(new FactorTemplate(new VarSet(v1, v2), alphabet, "key2"));
        }
        return fts;
    }
        
}
