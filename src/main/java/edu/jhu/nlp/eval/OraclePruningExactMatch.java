package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * Proportion of gold trees not pruned.
 * @author mgormley
 */
public class OraclePruningExactMatch implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(OraclePruningExactMatch.class);

    private boolean skipPunctuation;

    public OraclePruningExactMatch(boolean skipPunctuation) {
        this.skipPunctuation = skipPunctuation;
    }
    
    @Override
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name) {
        int numTot = 0;
        int numIncorrect = 0;
        for (int i=0; i<predSents.size(); i++) {
            AnnoSentence predSent = predSents.get(i);
            AnnoSentence goldSent = goldSents.get(i);
            for (int c=0; c<goldSent.size(); c++) {
                if (skipPunctuation && DepParseAccuracy.isPunctuation(goldSent.getWord(c))) {
                    // Don't score punctuation.
                    continue;
                }
                int p = goldSent.getParent(c);
                DepEdgeMask predMask = predSent.getDepEdgeMask();
                if (predMask != null && predMask.isPruned(p, c)) {
                    // A gold edge is pruned.
                    numIncorrect++;
                    break;
                }
            }
            numTot++;
        }
        int numCorrect = numTot - numIncorrect;
        log.info(String.format("Oracle pruning exact match on %s: %d / %d = %g", 
                name, numCorrect, numTot, (double) numCorrect / numTot));
        // Return the number of errors.
        return numTot - numCorrect;
    }

}
