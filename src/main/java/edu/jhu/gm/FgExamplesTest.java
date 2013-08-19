package edu.jhu.gm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.gm.CrfTrainerTest.SimpleVCFeatureExtractor;

public class FgExamplesTest {

    @Test
    public void testUpdatingOfTemplates() {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph(true);
        VarConfig vc = new VarConfig();
        for (Var v : fg.getVars()) {
            vc.put(v, 0);
        }
        
        FeatureTemplateList fts = new FeatureTemplateList();
        FgExample ex = new FgExample(fg, vc, new SimpleVCFeatureExtractor(fts));
        FgExamples data = new FgExamples(fts);
        data.add(ex);
        
        assertEquals(2, fts.size());
        assertEquals(1, fts.getNumObsFeats());
    }

}
