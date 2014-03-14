package edu.jhu.eval;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;

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

    public void evaluate(SimpleAnnoSentenceCollection gold, SimpleAnnoSentenceCollection pred) {
        correct = 0;
        total = 0;
        assert(pred.size() == gold.size());
        for (int i = 0; i < gold.size(); i++) {
            int[] goldParents = gold.get(i).getParents();
            int[] parseParents = pred.get(i).getParents();
            assert(parseParents.length == goldParents.length);
            for (int j = 0; j < goldParents.length; j++) {
                if (goldParents[j] == parseParents[j]) {
                    correct++;
                }
                total++;
            }
        }
        accuracy = (double) correct / (double) total;
        log.info(String.format("Unlabeled attachment score on %s: %.4f", dataName, accuracy));
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
