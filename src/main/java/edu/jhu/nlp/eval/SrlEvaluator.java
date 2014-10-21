package edu.jhu.nlp.eval;

import org.apache.log4j.Logger;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.util.report.Reporter;

/**
 * Computes the precision, recall, and micro-averaged F1 of SRL.
 * 
 * @author mgormley
 */
public class SrlEvaluator implements Evaluator {

    private static final Logger log = Logger.getLogger(SrlEvaluator.class);
    private static final Reporter rep = Reporter.getReporter(SrlEvaluator.class);
    private static final String NO_LABEL = "NO_SRL_LABEL_ON_THIS_EDGE";
        
    private double precision;
    private double recall;
    private double f1;
    
    /** Computes the precision, recall, and micro-averaged F1 of SRL. */
    public void evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String dataName) {
        log.warn("This evaluator is UNTESTED!!!");
        
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
            SrlGraph gold = goldSents.get(s).getSrlGraph();
            SrlGraph pred = predSents.get(s).getSrlGraph();
            
            if (gold == null) { continue; }
            if (pred == null) { numMissing++; }
            
            // For each gold edge.
            for (int pidx=0; pidx < goldSents.get(s).size(); pidx++) {
                // Only consider predicates which appear in the gold annotations.
                if (gold.getPredAt(pidx) == null) { continue; }
                for (int cidx=0; cidx < goldSents.get(s).size(); cidx++) {                          
                    SrlEdge goldEdge = gold.getEdge(pidx, cidx);
                    String goldLabel = (goldEdge == null) ? NO_LABEL : goldEdge.getLabel();
                    SrlEdge predEdge = pred.getEdge(pidx, cidx);
                    String predLabel = (predEdge == null) ? NO_LABEL : predEdge.getLabel();
                    
                    if (goldLabel.equals(predLabel)) {
                        if (!goldLabel.equals(NO_LABEL)) {
                            numCorrectPositive++;
                        } else {
                            numCorrectNegative++;
                        }
                    }
                    if (!NO_LABEL.equals(goldLabel)) {
                        numTruePositive++;
                    }
                    if (!NO_LABEL.equals(predLabel)) {
                        numPredictPositive++;
                    }
                    numInstances++;
                    log.trace(String.format("goldLabel=%s predLabel=%s", goldLabel, predLabel)); 
                }
            }
        }
        precision = numPredictPositive == 0 ? 0.0 : (double) numCorrectPositive / numPredictPositive;
        recall = numTruePositive == 0 ? 0.0 :  (double) numCorrectPositive / numTruePositive;
        f1 = (precision == 0.0 && recall == 0.0) ? 0.0 : (double) (2 * precision * recall) / (precision + recall);
        
        log.info(String.format("SRL Num sents not annotated on %s: %d", dataName, numMissing));
        log.info(String.format("SRL accuracy on %s: %.4f", dataName, (double)(numCorrectPositive + numCorrectNegative)/numInstances));
        log.info(String.format("SRL Num relation instances on %s: %d", dataName, numInstances));
        log.info(String.format("SRL Num true positives on %s: %d", dataName, numTruePositive));
        log.info(String.format("SRL Precision on %s: %.4f", dataName, precision));
        log.info(String.format("SRL Recall on %s: %.4f", dataName, recall));
        log.info(String.format("SRL F1 on %s: %.4f", dataName, f1));
        
        rep.report(dataName+"SrlF1", f1);
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
