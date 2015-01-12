package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * Proportion of gold trees not pruned.
 * @author mgormley
 */
public class DepParseExactMatch implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(DepParseExactMatch.class);

    private boolean skipPunctuation;

    public DepParseExactMatch(boolean skipPunctuation) {
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
                if (predSent.getParents() == null || predSent.getParent(c) != goldSent.getParent(c)) {
                    // A gold edge is incorrect.
                    numIncorrect++;
                    break;
                }
            }
            numTot++;
        }
        int numCorrect = numTot - numIncorrect;
        log.info("Unlabeled exact match on " + name + ": " + (double) numCorrect / numTot);
        // Return the number of errors.
        return numTot - numCorrect;
    }

}
