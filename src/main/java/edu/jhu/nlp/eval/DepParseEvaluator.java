package edu.jhu.nlp.eval;

import org.apache.log4j.Logger;

import edu.jhu.gm.app.Loss;
import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * Computes the unlabeled directed dependency accuracy. This is simply the
 * overall fraction of correctly predicted dependencies.
 * 
 * @author mgormley
 */
public class DepParseEvaluator implements Loss<AnnoSentence>, Evaluator {

    private static final Logger log = Logger.getLogger(DepParseEvaluator.class);

    private double accuracy;
    private int correct;
    private int total;

    /** Gets the number of incorrect dependencies. */
    @Override
    public double loss(AnnoSentence pred, AnnoSentence gold) {
        correct = 0;
        total = 0;
        evaluate(gold, pred);
        return total-correct;
    }  

    /** Computes the number of correct dependencies, total dependencies, and accuracy. */
    public void evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String dataName) {
        correct = 0;
        total = 0;
        assert(predSents.size() == goldSents.size());
        for (int i = 0; i < goldSents.size(); i++) {
            AnnoSentence gold = goldSents.get(i);
            AnnoSentence pred = predSents.get(i);
            evaluate(gold, pred);
        }
        accuracy = (double) correct / (double) total;
        log.info(String.format("Unlabeled attachment score on %s: %.4f", dataName, accuracy));
    }

    private void evaluate(AnnoSentence gold, AnnoSentence pred) {
        int[] goldParents = gold.getParents();
        int[] parseParents = pred.getParents();
        if (parseParents != null) {
            assert(parseParents.length == goldParents.length);
        }
        for (int j = 0; j < goldParents.length; j++) {
            if (parseParents != null) {
                if (goldParents[j] == parseParents[j]) {
                    correct++;
                }
            }
            total++;
        }
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getCorrect() {
        return correct;
    }

    public int getTotal() {
        return total;
    }  

}
