package edu.jhu.nlp.depparse;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.depparse.BitshiftDepParseFeatureExtractor.BitshiftDepParseFeatureExtractorPrm;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.features.TemplateSets;
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
        // Construct a dummy feature extractor with null values.
        return get1stOrderFg(sent, null, null, 0, true);
    }
    
    public static UFgExample get1stOrderFg(AnnoSentence sent, CorpusStatistics cs, ObsFeatureConjoiner ofc, int numParams, boolean onlyFast) {
        FactorGraph fg = new FactorGraph();
        DepParseFeatureExtractorPrm fePrm = new DepParseFeatureExtractorPrm();        
        fePrm.featureHashMod = numParams;
        fePrm.firstOrderTpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        BitshiftDepParseFeatureExtractorPrm bsFePrm = new BitshiftDepParseFeatureExtractorPrm();
        bsFePrm.featureHashMod = numParams;
        FeatureExtractor fe = onlyFast?
                new BitshiftDepParseFeatureExtractor(bsFePrm, sent, cs, ofc) :
                new DepParseFeatureExtractor(fePrm, sent, cs, ofc.getFeAlphabet());
        
        DepParseFactorGraphBuilderPrm fgPrm = new DepParseFactorGraphBuilderPrm();
        fgPrm.useProjDepTreeFactor = true;
        fgPrm.grandparentFactors = false;
        fgPrm.siblingFactors = false;
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(fgPrm);
        builder.build(sent, fe, fg);
        
        UnlabeledFgExample ex = new UnlabeledFgExample(fg, new VarConfig());
        return ex;
    }
    
    public static void main(String[] args) {
        (new DepParseFactorGraphBuilderSpeedTest()).testSpeed();
    }
    
}
