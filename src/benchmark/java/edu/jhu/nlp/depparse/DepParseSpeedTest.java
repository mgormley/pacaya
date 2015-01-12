package edu.jhu.nlp.depparse;

import edu.jhu.autodiff.erma.ErmaBp;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Timer;

public class DepParseSpeedTest {
    
    // TODO: Try with ErmaBpPrm.keepTape = false.
    /**
     * Speed test results.
     * 
     * t1: Build factor graph
     * t3: the feature extraction and dot product
     * t4: Run BP
     * 
     * For numParams = 100,000  
     * on 11/03/14        s=2401 n=56427 tot= 848.30 t0=178566.46 t1=4282.88 t2=6269666.67 t3=6388.20 t4=1355.51 t5=22197.88
     * w/no interning of Var.name, FastMath.logAddTable=true, bpPrm.s = Algebras.REAL_ALGEBRA:
     *                    s=2401 n=56427 tot=1220.41 t0=191277.97 t1=5084.89 t2=28213500.00 t3=6462.09 t4=2346.24 t5=27565.71
     * w/coarse POS tag feats:
     *                    s=2401 n=56427 tot=1115.82 t0=122667.39 t1=5162.58 t2=28213500.00 t3=3775.64 t4=2578.11 t5=24124.41
     *
     * BELOW ARE WITHOUT PROJDEPTREEGLOBALFACTOR.
     * w/Narad.StrFeats   s=2401 n=56427 Toks / sec: 662.01
     * w/McDon.StrFeats   s=2401 n=56427 tot= 204.06 t1=4300.51 t2=11285400.00 t3= 245.32 t4=1778.12 t5=35355.26
     * w/IntFeats         s=2401 n=56427 tot= 868.67 t1=4592.79 t2=14106750.00 t3=2922.32 t4=1776.28 t5=35849.43
     * w/DirectToFv       s=2401 n=56427 tot= 931.51 t0=217026.92 t1=5106.98 t2=11285400.00 t3=3332.37 t4=1830.98 t5=37643.10
     *    
     * For numParams = 1,000,000
     * w/IntFeats         s=2401 n=56427 tot= 831.90 t1=4307.40 t2=28213500.00 t3=2727.52 t4=1742.17 t5=35069.61
     * 
     * For numParams = 1
     * w/IntFeats         s=2401 n=56427 tot=1052.02 t1=4293.31 t2=28213500.00 t3=9267.04 t4=1723.54 t5=34681.62
     *
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
                UFgExample ex = DepParseFactorGraphBuilderSpeedTest.get1stOrderFg(sent, cs, alphabet, numParams, onlyFast);
                t1.stop();
                
                t2.start();
                FactorGraph fg = ex.getFgLatPred();
                t2.stop();
                
                t3.start(); 
                fg.updateFromModel(model);
                t3.stop();
                
                t4.start(); 
                ErmaBp bp = DepParseInferenceSpeedTest.runBp(fg);
                t4.stop();
                
                t5.start(); 
                DepParseDecoder decode = new DepParseDecoder();
                int[] parents = decode.decode(bp, ex, sent);     
                s += parents[0];
                s -= parents[0];
                t5.stop();
                
                n+=sent.size();
                if (s++%100 == 0) {
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
    
    public static void main(String[] args) {
        (new DepParseSpeedTest()).testSpeed();
    }
    
}
