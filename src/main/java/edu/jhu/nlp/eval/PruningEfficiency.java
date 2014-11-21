package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/** 
 * Computes the proportion of non-gold first-order dependency arcs pruned.
 * @author mgormley
 */
public class PruningEfficiency implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(PruningEfficiency.class);

    private boolean skipPunctuation;

    public PruningEfficiency(boolean skipPunctuation) {
        this.skipPunctuation = skipPunctuation;
    }
    
    @Override
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name) {
        int numTot = 0;
        int numPruned = 0;
        for (int i=0; i<predSents.size(); i++) {
            AnnoSentence predSent = predSents.get(i);
            AnnoSentence goldSent = goldSents.get(i);
            for (int p=-1; p<goldSent.size(); p++) {
                for (int c=0; c<goldSent.size(); c++) {
                    if (p == c) { continue; }
                    if (goldSent.getParent(c) == p) {
                        // Don't count gold edges.
                        continue; 
                    }
                    if (skipPunctuation && DepParseAccuracy.isPunctuation(goldSent.getWord(c))) {
                        // Don't score punctuation.
                        continue;
                    }
                    if (predSent.getDepEdgeMask() != null && predSent.getDepEdgeMask().isPruned(p, c)) {
                        numPruned++;
                    }
                    numTot++;
                }
            }
        }
        log.info("Pruning efficiency on " + name + ": " + (double) numPruned / numTot);
        // Return the number of non-gold edges kept.
        return numTot - numPruned;
    }

}
