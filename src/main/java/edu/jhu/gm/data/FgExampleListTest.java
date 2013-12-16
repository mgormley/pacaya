package edu.jhu.gm.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor;

public class FgExampleListTest {

    @Test
    public void testUpdatingOfTemplates() {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph(true);
        VarConfig vc = new VarConfig();
        for (Var v : fg.getVars()) {
            vc.put(v, 0);
        }
        
        FactorTemplateList fts = new FactorTemplateList();
        FgExample ex = new FgExample(fg, vc, new SimpleVCFeatureExtractor(fg, vc, fts), fts);
        FgExampleMemoryStore data = new FgExampleMemoryStore(fts);
        data.add(ex);
        
        System.out.println(fts);
        
        assertEquals(2, fts.size());
        // One for each template.
        assertEquals(2, fts.getNumObsFeats());
    }

}
