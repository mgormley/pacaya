package edu.jhu.nlp.eval;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DepParseAccuracy implements Loss<AnnoSentence>, Evaluator {

    /** Regex for matching words consisting of entirely Unicode punctuation characters. */
    static final Pattern PUNCT_RE = Pattern.compile("^\\p{Punct}+$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Logger log = LoggerFactory.getLogger(DepParseAccuracy.class);

    private double accuracy;
    private int correct;
    private int total;
    private boolean skipPunctuation;

    public DepParseAccuracy(boolean skipPunctuation) {
        this.skipPunctuation = skipPunctuation;
    }
    
    /** Gets the number of incorrect dependencies. */
    @Override
    public double loss(AnnoSentence pred, AnnoSentence gold) {
        correct = 0;
        total = 0;
        evaluate(pred, gold);
        return getErrors();
    }

    /** Computes the number of correct dependencies, total dependencies, and accuracy. */
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String dataName) {
        correct = 0;
        total = 0;
        assert(predSents.size() == goldSents.size());
        for (int i = 0; i < goldSents.size(); i++) {
            AnnoSentence gold = goldSents.get(i);
            AnnoSentence pred = predSents.get(i);
            evaluate(pred, gold);
        }
        accuracy = (double) correct / (double) total;
        log.info(String.format("Unlabeled attachment score on %s: %.4f", dataName, accuracy));        
        return getErrors();
    }

    private void evaluate(AnnoSentence pred, AnnoSentence gold) {
        int[] goldParents = gold.getParents();
        int[] parseParents = pred.getParents();
        if (parseParents != null) {
            assert(parseParents.length == goldParents.length);
        }
        for (int c = 0; c < goldParents.length; c++) {
            if (skipPunctuation && isPunctuation(gold.getWord(c))) {
                // Don't score punctuation.
                continue;
            }
            if (parseParents != null) {
                if (goldParents[c] == parseParents[c]) {
                    correct++;
                }
            }
            total++;            
        }
    }
    
    public static boolean isPunctuation(String word) {
        return PUNCT_RE.matcher(word).matches();
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

    public double getErrors() {
        return total - correct;
    }

}
