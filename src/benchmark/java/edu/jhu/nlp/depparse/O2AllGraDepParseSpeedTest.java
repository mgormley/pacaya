package edu.jhu.nlp.depparse;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebras;

/**
 * Speed test for 2nd order all grandparents inference using either BP or dynamic programming.
 */
public class O2AllGraDepParseSpeedTest {
    
    /**
     * Speed test results.
     * 
     * t1: Build factor graph
     * t3: the feature extraction and dot product
     * t4: Run inference
     * 
     * With numParams = 100000:
     * BP (iters=1): s=111 n=2610 tot=  31.66 t0=2394.50 t1= 728.03 t2=Infinity t3= 692.68 t4=  35.33 t5=20880.00
     * BP (iters=5): s=111 n=2610 tot=  16.30 t0=2824.68 t1= 505.72 t2=Infinity t3= 578.20 t4=  17.47 t5=20880.00
     * DP:           s=111 n=2610 tot= 143.34 t0=2305.65 t1= 543.75 t2=Infinity t3=1015.17 t4= 274.30 t5=13957.22
     */
    //@Test
    public void testSpeed() {
        FastMath.useLogAddTable = true;
        for (int trial = 0; trial < 2; trial++) {
            Timer t = new Timer();
            Timer t0 = new Timer();
            Timer t1 = new Timer();
            Timer t2 = new Timer();
            Timer t3 = new Timer();
            Timer t4 = new Timer();
            Timer t5 = new Timer();
            
            t.start();
            
            t0.start();
            AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
            t0.stop();

            // Don't time this stuff since it's "training".
            t.stop();
            int numParams = 100000;
            FgModel model = new FgModel(numParams);
            CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
            cs.init(sents);
            FeatureNames alphabet = new FeatureNames();
            boolean onlyFast = true;
            t.start();
            
            int s=0;
            int n=0;
    
            for (AnnoSentence sent : sents) {
                t1.start(); 
                UFgExample ex = get2ndOrderFg(sent, cs, alphabet, numParams, onlyFast);
                t1.stop();
                
                t2.start();
                FactorGraph fg = ex.getFgLatPred();
                t2.stop();
                
                t3.start(); 
                fg.updateFromModel(model);
                t3.stop();
                
                t4.start(); 
                FgInferencer bp = null;
                //System.out.println("size:"+sent.size());
                //for (int i=0; i<1000; i++) {
                if (true) {
                    bp = DepParseInferenceSpeedTest.runBp(fg, 5);
                } else {
                    bp = new O2AllGraFgInferencer(fg, Algebras.LOG_SEMIRING);
                    bp.run();
                }
                //}
                t4.stop();
                
                t5.start(); 
                DepParseDecoder decode = new DepParseDecoder();
                int[] parents = decode.decode(bp, ex, sent);     
                s += parents[0];
                s -= parents[0];
                t5.stop();
                
                n+=sent.size();
                if (s++%10 == 0) {
                    t.stop();
                    System.out.println(String.format("s=%d n=%d tot=%7.2f t0=%7.2f t1=%7.2f t2=%7.2f t3=%7.2f t4=%7.2f t5=%7.2f", s, n, 
                            (n/t.totSec()), 
                            (n/t0.totSec()),
                            (n/t1.totSec()),
                            (n/t2.totSec()),
                            (n/t3.totSec()),
                            (n/t4.totSec()),
                            (n/t5.totSec()))
                            );
                    t.start();
                }
            }
            t.stop();
            
            System.out.println("Total secs: " + t.totSec());
            System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
        }
    }
    
    public static UFgExample get2ndOrderFg(AnnoSentence sent, CorpusStatistics cs, FeatureNames alphabet, int numParams, boolean onlyFast) {
        FactorGraph fg = new FactorGraph();
        DepParseFeatureExtractorPrm fePrm = new DepParseFeatureExtractorPrm();
        fePrm.featureHashMod = numParams;
        fePrm.firstOrderTpls = TemplateSets.getFromResource(TemplateSets.mcdonaldDepFeatsResource);
        FeatureExtractor fe = onlyFast?
                new BitshiftDepParseFeatureExtractor(sent, cs, numParams, alphabet) :
                new DepParseFeatureExtractor(fePrm, sent, cs, alphabet);
        
        DepParseFactorGraphBuilderPrm fgPrm = new DepParseFactorGraphBuilderPrm();
        fgPrm.useProjDepTreeFactor = true;        
        fgPrm.grandparentFactors = true;
        fgPrm.siblingFactors = false;    
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(fgPrm);
        builder.build(sent, fe, fg);
        
        UnlabeledFgExample ex = new UnlabeledFgExample(fg, new VarConfig());
        return ex;
    }
    
    public static void main(String[] args) {
        (new O2AllGraDepParseSpeedTest()).testSpeed();
    }
    
}
