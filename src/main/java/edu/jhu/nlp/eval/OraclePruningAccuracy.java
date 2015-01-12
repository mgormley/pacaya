package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public class OraclePruningAccuracy implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(OraclePruningAccuracy.class);

    private boolean skipPunctuation;

    public OraclePruningAccuracy(boolean skipPunctuation) {
        this.skipPunctuation = skipPunctuation;
    }
    
    @Override
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name) {
        int numTot = 0;
        int numCorrect = 0;
        for (int i=0; i<predSents.size(); i++) {
            AnnoSentence predSent = predSents.get(i);
            AnnoSentence goldSent = goldSents.get(i);
            for (int c=0; c<goldSent.size(); c++) {
                if (skipPunctuation && DepParseAccuracy.isPunctuation(goldSent.getWord(c))) {
                    // Don't score punctuation.
                    continue;
                }
                int p = goldSent.getParent(c);
                if (predSent.getDepEdgeMask() == null || predSent.getDepEdgeMask().isKept(p, c)) {
                    numCorrect++;
                }
                numTot++;
            }
        }
        log.info("Oracle pruning accuracy on " + name + ": " + (double) numCorrect / numTot);
        // Return the number of errors.
        return numTot - numCorrect;
    }

}
