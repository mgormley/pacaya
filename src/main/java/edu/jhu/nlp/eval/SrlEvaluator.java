package edu.jhu.nlp.eval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.Prm;
import edu.jhu.util.report.Reporter;

/**
 * Computes the precision, recall, and micro-averaged F1 of SRL.
 * 
 * @author mgormley
 */
// TODO: Support other options: predictSense = true, predictPredicatePosition = true.
public class SrlEvaluator implements Evaluator {

    public static class SrlEvaluatorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public boolean labeled = true;
        public boolean predictSense = false;
        public boolean predictPredicatePosition = false;
    }
    
    private static final Logger log = LoggerFactory.getLogger(SrlEvaluator.class);
    private static final Reporter rep = Reporter.getReporter(SrlEvaluator.class);
    private static final String NO_LABEL = "NO_SRL_LABEL_ON_THIS_EDGE";
    private static final String SOME_LABEL = "SOME_SRL_LABEL_ON_THIS_EDGE";

    private final SrlEvaluatorPrm prm;

    private double precision;
    private double recall;
    private double f1;    
    
    public SrlEvaluator() {
        this.prm = new SrlEvaluatorPrm();
        prm.predictPredicatePosition = false;
        prm.predictSense = false;
    }
    
    /**
     * @param labeled Whether to compute labeled or unlabeled F1.
     */
    public SrlEvaluator(SrlEvaluatorPrm prm) {
        if (prm.predictSense || prm.predictPredicatePosition) {
            throw new IllegalArgumentException("Unsupported options");
        }
        this.prm = prm;
    }
    
    /** Computes the precision, recall, and micro-averaged F1 of SRL. */
    public double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String dataName) {
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
            int n = goldSents.get(s).size();
            for (int p=0; p < n; p++) {
                // Only consider predicates which appear in the gold annotations.
                if (gold.getPredAt(p) == null) { continue; }
                for (int c=0; c < n; c++) {                          
                    SrlEdge goldEdge = gold.getEdge(p, c);
                    String goldLabel = (goldEdge == null) ? NO_LABEL : 
                        (!prm.labeled ? SOME_LABEL : goldEdge.getLabel());
                    SrlEdge predEdge = pred.getEdge(p, c);
                    String predLabel = (predEdge == null) ? NO_LABEL : 
                        (!prm.labeled ? SOME_LABEL : predEdge.getLabel());
                    
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
        
        final String lu = prm.labeled ? "Labeled" : "Unlabeled";
        log.info(String.format("SRL Num sents not annotated on %s: %d", dataName, numMissing));
        log.info(String.format("SRL Num relation instances on %s: %d", dataName, numInstances));
        log.info(String.format("SRL %s accuracy on %s: %.4f", lu, dataName, (double)(numCorrectPositive + numCorrectNegative)/numInstances));
        log.info(String.format("SRL %s Num true positives on %s: %d", lu, dataName, numTruePositive));
        log.info(String.format("SRL %s Precision on %s: %.4f", lu, dataName, precision));
        log.info(String.format("SRL %s Recall on %s: %.4f", lu, dataName, recall));
        log.info(String.format("SRL %s F1 on %s: %.4f", lu, dataName, f1));
        
        rep.report(dataName+lu+"SrlPrecision", precision);
        rep.report(dataName+lu+"SrlRecall", recall);
        rep.report(dataName+lu+"SrlF1", f1);
        
        return -f1;
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
