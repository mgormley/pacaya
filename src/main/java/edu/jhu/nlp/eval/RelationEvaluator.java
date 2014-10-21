package edu.jhu.nlp.eval;

import org.apache.log4j.Logger;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.util.report.Reporter;

/**
 * Computes the precision, recall, and micro-averaged F1 of relations mentions.
 * 
 * @author mgormley
 */
public class RelationEvaluator implements Evaluator {

    private static final Logger log = Logger.getLogger(RelationEvaluator.class);
    private static final Reporter rep = Reporter.getReporter(RelationEvaluator.class);

    private double precision;
    private double recall;
    private double f1;
    
    /** Computes the precision, recall, and micro-averaged F1 of relations mentions. */
    public void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents, String dataName) {
        // Precision = # correctly predicted positive / # predicted positive
        // Recall = # correctly predicted positive / # true positive
        int numCorrectPositive = 0;
        int numCorrectNegative = 0;
        int numPredictPositive = 0;
        int numTruePositive = 0;
        int numInstances = 0;
        int numMissing = 0;
        
        assert predSents.size() == goldSents.size();
        
        // For each sentence.
        for (int s = 0; s < goldSents.size(); s++) {
            AnnoSentence gold = goldSents.get(s);
            AnnoSentence pred = predSents.get(s);
            
            if (pred.getRelLabels() == null) {
                numMissing++;
            }
            // For each pair of named entities.
            for (int k=0; k<gold.getRelLabels().size(); k++) {                
                String goldLabel = gold.getRelLabels().get(k);
                String predLabel = (pred.getRelLabels() == null) ? null : pred.getRelLabels().get(k);
                
                if (goldLabel.equals(predLabel)) {
                    if (!goldLabel.equals(RelationsEncoder.getNoRelationLabel())) {
                        numCorrectPositive++;
                    } else {
                        numCorrectNegative++;
                    }
                }
                if (!RelationsEncoder.getNoRelationLabel().equals(goldLabel)) {
                    numTruePositive++;
                }
                if (!RelationsEncoder.getNoRelationLabel().equals(predLabel)) {
                    numPredictPositive++;
                }
                numInstances++;
                log.trace(String.format("goldLabel=%s predLabel=%s", goldLabel, predLabel));                    
            }
        }
        precision = numPredictPositive == 0 ? 0.0 : (double) numCorrectPositive / numPredictPositive;
        recall = numTruePositive == 0 ? 0.0 :  (double) numCorrectPositive / numTruePositive;
        f1 = (precision == 0.0 && recall == 0.0) ? 0.0 : (double) (2 * precision * recall) / (precision + recall);
        
        log.info(String.format("Num sents not annotated on %s: %d", dataName, numMissing));
        log.info(String.format("Relation accuracy on %s: %.4f", dataName, (double)(numCorrectPositive + numCorrectNegative)/numInstances));
        log.info(String.format("Num relation instances on %s: %d", dataName, numInstances));
        log.info(String.format("Num true positives on %s: %d", dataName, numTruePositive));
        log.info(String.format("Precision on %s: %.4f", dataName, precision));
        log.info(String.format("Recall on %s: %.4f", dataName, recall));
        log.info(String.format("F1 on %s: %.4f", dataName, f1));
        
        rep.report(dataName+"F1", f1);
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getF1() {
        return f1;
    }
    
}
