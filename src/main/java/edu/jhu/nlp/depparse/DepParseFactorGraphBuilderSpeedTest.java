package edu.jhu.nlp.depparse;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.util.Timer;

public class DepParseFactorGraphBuilderSpeedTest {
    
    /**
     * Speed test results:
     * Tokens / sec: 8028.895184135978
     */
    //@Test
    public void testSpeed() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        
        Timer t = new Timer();
        t.start();
        for (AnnoSentence sent : sents) {
            UFgExample ex = get1stOrderFg(sent);
            ex.getFgLatPred();
        }
        t.stop();
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }

    public static UFgExample get1stOrderFg(AnnoSentence sent) {
        FactorGraph fg = new FactorGraph();
        // Construct a dummy feature extractor with null values.
        FeatureExtractor fe = new DepParseFeatureExtractor(null, sent, null, null);
        
        DepParseFactorGraphBuilderPrm prm = new DepParseFactorGraphBuilderPrm();
        prm.grandparentFactors = false;
        prm.siblingFactors = false;            
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(prm);
        builder.build(sent, fe, fg);
        
        UnlabeledFgExample ex = new UnlabeledFgExample(fg, new VarConfig());
        return ex;
    }
    
    public static void main(String[] args) {
        (new DepParseFactorGraphBuilderSpeedTest()).testSpeed();
    }
    
}
