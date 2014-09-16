package edu.jhu.nlp.depparse;

import edu.jhu.autodiff.erma.ErmaBp;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.util.Timer;

public class DepParseDecoderSpeedTest {
    
    /**
     * Speed test results.
     * 
     * If we comment out the IllegalStateException in ExpFamFactor to avoid updating from a model:
     *   Total secs: 1.736
     *   Tokens / sec: 32652.073732718894
     */
    //@Test
    public void testSpeed() {
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        
        Timer t = new Timer();
        int s=0;
        int n=0;
        for (AnnoSentence sent : sents) {
            UFgExample ex = DepParseFactorGraphBuilderSpeedTest.get1stOrderFg(sent);
            FactorGraph fg = ex.getFgLatPred();
            ErmaBp bp = DepParseInferenceSpeedTest.runBp(fg);
            
            t.start();
            DepParseDecoder decode = new DepParseDecoder();
            int[] parents = decode.decode(bp, ex, sent);     
            s += parents[0];
            s -= parents[0];
            t.stop();
            
            n+=sent.size();
            if (s++%100 == 0) {
                System.out.println("Tokens / sec: " + (n / t.totSec()));
            }
        }
        System.out.println("Total secs: " + t.totSec());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }
    
    public static void main(String[] args) {
        (new DepParseDecoderSpeedTest()).testSpeed();
    }
    
}
