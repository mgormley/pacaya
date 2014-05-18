package edu.jhu.eval;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;

/**
 * Computes the unlabeled directed dependency accuracy. This is simply the
 * overall fraction of correctly predicted dependencies.
 * 
 * @author mgormley
 */
public class DepParseEvaluator {

    private static final Logger log = Logger.getLogger(DepParseEvaluator.class);

    private String dataName;

    private double accuracy;
    private int correct;
    private int total;

    public DepParseEvaluator(String dataName) {
        this.dataName = dataName;
    }

    /** Gets the number of incorrect dependencies. */
    public double loss(AnnoSentence pred, AnnoSentence gold) {
        correct = 0;
        total = 0;
        evaluate(gold, pred);
        return total-correct;
    }  

    /** Computes the number of correct dependencies, total dependencies, and accuracy. */
    public void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents) {
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
        assert(parseParents.length == goldParents.length);
        for (int j = 0; j < goldParents.length; j++) {
            if (goldParents[j] == parseParents[j]) {
                correct++;
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
