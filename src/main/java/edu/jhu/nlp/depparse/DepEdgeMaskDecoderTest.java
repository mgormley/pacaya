package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepEdgeMaskDecoder.DepEdgeMaskDecoderPrm;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.util.collections.Lists;

public class DepEdgeMaskDecoderTest {

    EdgeScores scores;
    FgInferencer inf;
    FactorGraph fg;
    int n;
    
    @Before
    public void setUp() {
        DepParseDecoderTest dpdt = new DepParseDecoderTest();
        dpdt.setUp();
        this.scores = dpdt.scores;
        this.inf = dpdt.inf;
        this.fg = dpdt.fg;
        this.n = dpdt.n;
    }
    
    @Test
    public void testGetDepEdgeMaskPropMaxMarg() {
        double propMaxMarg = 0.1;
        DepEdgeMask mask = DepEdgeMaskDecoder.getDepEdgeMask(scores, propMaxMarg, 99, true);
        System.out.println(mask);
        assertNoneArePruned(mask);
        
        propMaxMarg = 0.5;
        mask = DepEdgeMaskDecoder.getDepEdgeMask(scores, propMaxMarg, 99, true);
        System.out.println(mask);
        assertOnlyMostLikelyAreKept(mask);
    }
    
    @Test
    public void testGetDepEdgeMaskCount() {
        DepEdgeMask mask = DepEdgeMaskDecoder.getDepEdgeMask(scores, 0, 99, true);
        System.out.println(mask);
        assertNoneArePruned(mask);
        
        mask = DepEdgeMaskDecoder.getDepEdgeMask(scores, 0, 1, true);
        System.out.println(mask);
        assertOnlyMostLikelyAreKept(mask);
    }
    
    @Test
    public void testDecoder() {
        DepEdgeMaskDecoderPrm prm = new DepEdgeMaskDecoderPrm();
        prm.maxPrunedHeads = 99;
        prm.pruneMargProp = 0;                
        DepEdgeMaskDecoder dp = new DepEdgeMaskDecoder(prm);
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList("a", "b", "c"));
        DepEdgeMask mask = dp.decode(inf, new UnlabeledFgExample(fg, new VarConfig()), sent);
        assertNoneArePruned(mask);
    }

    private void assertOnlyMostLikelyAreKept(DepEdgeMask mask) {
        assertEquals(3, mask.getCount());

        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                if ((p == -1 && c == 1) || 
                        (p == 1 && c == 0) || 
                        (p == 1 && c == 2)) {
                    assertTrue("pc: "+p + " " + c, mask.isKept(p, c));
                    assertTrue("pc: "+p + " " + c, !mask.isPruned(p, c));
                } else {
                    assertTrue("pc: "+p + " " + c, !mask.isKept(p, c));
                    assertTrue("pc: "+p + " " + c, mask.isPruned(p, c));
                }
            }
        }
    }

    private void assertNoneArePruned(DepEdgeMask mask) {
        assertEquals(n*n, mask.getCount());

        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                assertTrue("pc: "+p + " " + c, mask.isKept(p, c));
                assertTrue("pc: "+p + " " + c, !mask.isPruned(p, c));
            }
        }
    }

}
