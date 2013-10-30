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
        FgExample ex = new FgExample(fg, vc, new SimpleVCFeatureExtractor(fg, vc, fts), fts);
        FgExamplesMemoryStore data = new FgExamplesMemoryStore(fts);
        data.add(ex);
        
        System.out.println(fts);
        
        assertEquals(2, fts.size());
        // One for each template.
        assertEquals(2, fts.getNumObsFeats());
    }

}
