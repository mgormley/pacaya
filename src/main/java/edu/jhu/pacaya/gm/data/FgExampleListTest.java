package edu.jhu.pacaya.gm.data;

import static org.junit.Assert.assertEquals;
import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphTest;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.train.CrfTrainerTest.SimpleVCObsFeatureExtractor;

public class FgExampleListTest {

    // TODO: Move this test. It doesn't really belong here. @Test
    public void testUpdatingOfTemplates() {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph();
        VarConfig vc = new VarConfig();
        for (Var v : fg.getVars()) {
            vc.put(v, 0);
        }
        
        FactorTemplateList fts = new FactorTemplateList();
        LFgExample ex = new LabeledFgExample(fg, vc, new SimpleVCObsFeatureExtractor(fts), fts);
        FgExampleMemoryStore data = new FgExampleMemoryStore();
        data.add(ex);
        
        System.out.println(fts);
        
        assertEquals(2, fts.size());
        // One for each template.
        assertEquals(2, fts.getNumObsFeats());
    }

}
