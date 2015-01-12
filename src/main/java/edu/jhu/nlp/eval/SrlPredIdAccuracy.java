package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.report.Reporter;

/**
 * Evaluates the accuracy of predicate identification for SRL.
 * @author mgormley
 */
public class SrlPredIdAccuracy implements Evaluator {

    private static final Logger log = LoggerFactory.getLogger(SrlPredIdAccuracy.class);
    private static final Reporter rep = Reporter.getReporter(SrlPredIdAccuracy.class);

    @Override
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name) {
        int correct = 0;
        int total = 0;
        assert(predSents.size() == goldSents.size());
        for (int i = 0; i < goldSents.size(); i++) {
            AnnoSentence gold = goldSents.get(i);
            AnnoSentence pred = predSents.get(i);
            if (gold.getKnownPreds() != null) {
                total += gold.size();
                if (pred.getKnownPreds() != null) {
                    for (int j=0; j<gold.size(); j++) {
                        if (gold.isKnownPred(j) == pred.isKnownPred(j)) {
                            correct++;
                        }
                    }
                }
            }
        }
        double accuracy = (double) correct / (double) total;
        log.info(String.format("SRL predicate ID accuracy on %s: %.4f", name, accuracy));    
        rep.report(name+"SrlPredIdAccuracy", accuracy);
        return total - correct;
    }

}
