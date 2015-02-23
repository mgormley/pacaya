package edu.jhu.nlp.depparse;

import edu.jhu.autodiff.erma.ErmaBp;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.hypergraph.depparse.DepParseFirstVsSecondOrderTest;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.tag.StrictPosTagAnnotator;
import edu.jhu.nlp.words.PrefixAnnotator;
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
     * Key:
     *   ta = Log-add table, ex = Exact log-add
     *   L = LOG_SEMIRING, R = REAL_ALGEGRA , S = LOG_SIGN_ALGEBRA
     *   co = coarse POS tag feats, nc = no coarse tag feats
     *   MST = MST features, TUR = Turbo Parser feats
     *   hp = HPROF running, nh = no HPROF
     *   gr = grandparent factors, as = arbitrary sibling
     *   i# = number of iterations
     *   
     * For numParams = 100,000  
     * on 11/03/14        s=2401 n=56427 tot= 848.30 t0=178566.46 t1=4282.88 t2=6269666.67 t3=6388.20 t4=1355.51 t5=22197.88
     * w/no interning of Var.name, FastMath.logAddTable=true, bpPrm.s = Algebras.REAL_ALGEBRA:
     *                    s=2401 n=56427 tot=1220.41 t0=191277.97 t1=5084.89 t2=28213500.00 t3=6462.09 t4=2346.24 t5=27565.71
     * w/coarse POS tag feats:
     *                    s=2401 n=56427 tot=1115.82 t0=122667.39 t1=5162.58 t2=28213500.00 t3=3775.64 t4=2578.11 t5=24124.41
     * on 02/22/14:
     * ta,L,nc,MST,hp     s=2401 n=56427 tot=1239.85 t0=154172.13 t1=5518.53 t2=14106750.00 t3=5524.48 t4=2485.44 t5=28541.73
     * ta,L,co,MST,hp     s=2401 n=56427 tot=1048.17 t0=141776.38 t1=5585.73 t2=18809000.00 t3=3059.70 t4=2485.66 t5=26015.21
     * ta,L,co,MST,nh     s=2401 n=56427 tot=1092.15 t0=184401.96 t1=5446.10 t2=11285400.00 t3=2997.61 t4=2764.00 t5=32392.08
     * ex,L,co,MST,hp     s=2401 n=56427 tot= 696.26 t0=160303.98 t1=5468.79 t2=56427000.00 t3=2425.51 t4=1263.79 t5=23211.44
     * ta,S,co,MST,hp     s=2401 n=56427 tot=1131.05 t0=136627.12 t1=5934.06 t2=11285400.00 t3=3409.69 t4=2623.29 t5=29950.64
     *
     *
     * ============
     * 2nd-order
     * ============
     * 
     * gr,as,i4            s=701 n=16862 tot=  69.35 t0=16794.82 t1=2199.58 t2=3372400.00 t3=2782.51 t4=  74.11 t5=19493.64
     * (same no inference) s=701 n=16862 tot=1069.65 t0=16794.82 t1=2066.67 t2=Infinity t3=2557.56 t4=Infinity t5=Infinity
     * (same elemMultiply) s=701 n=16862 tot=  87.08 t0=15329.09 t1=2286.99 t2=2810333.33 t3=2695.76 t4=  94.69 t5=20791.62
     */
    //@Test
    public void testSpeed() {
        FastMath.useLogAddTable = true;
        boolean firstOrder = false;
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
            PrefixAnnotator.addPrefixes(sents);
            StrictPosTagAnnotator.addStrictPosTags(sents);           
            t0.stop();

            // Don't time this stuff since it's "training".
            t.stop();
            PosTagDistancePruner pruner = new PosTagDistancePruner();
            pruner.train(sents, sents, null, null);
            pruner.annotate(sents);
            
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
                UFgExample ex = firstOrder ?
                        DepParseFactorGraphBuilderSpeedTest.get1stOrderFg(sent, cs, alphabet, numParams, onlyFast) :
                        DepParseFirstVsSecondOrderTest.get2ndOrderFg(sent, cs, alphabet, numParams, onlyFast);
                t1.stop();
                
                t2.start();
                FactorGraph fg = ex.getFgLatPred();
                t2.stop();
                
                t3.start(); 
                fg.updateFromModel(model);
                t3.stop();
                
                t4.start(); 
                ErmaBp bp = firstOrder ?
                        DepParseInferenceSpeedTest.runBp(fg, 1) :
                        DepParseInferenceSpeedTest.runBp(fg, 4);
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
