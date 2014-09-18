package edu.jhu.nlp.eval;

import org.apache.log4j.Logger;

import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.prim.tuple.Pair;

/**
 * Computes the precision, recall, and micro-averaged F1 of relations mentions.
 * 
 * @author mgormley
 */
public class RelationEvaluator {

    private static final Logger log = Logger.getLogger(RelationEvaluator.class);

    private String dataName;

    private double precision;
    private double recall;
    private double f1;

    public RelationEvaluator(String dataName) {
        this.dataName = dataName;
    }
    
    /** Computes the precision, recall, and micro-averaged F1 of relations mentions. */
    public void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents) {
        // Precision = # correctly predicted positive / # predicted positive
        // Recall = # correctly predicted positive / # true positive
        int numCorrectPositive = 0;
        int numCorrectNegative = 0;
        int numPredictPositive = 0;
        int numTruePositive = 0;
        int numInstances = 0;

        assert predSents.size() == goldSents.size();
        
        // For each sentence.
        for (int s = 0; s < goldSents.size(); s++) {
            AnnoSentence gold = goldSents.get(s);
            AnnoSentence pred = predSents.get(s);
            
            RelationMentions goldRels = gold.getRelations();
            RelationMentions predRels = pred.getRelations();
            
            // For each pair of named entities.
        	for (Pair<NerMention,NerMention> pair : pred.getNePairs()) {
        		NerMention ne1 = pair.get1();
        		NerMention ne2 = pair.get2();
                
                String goldLabel = RelationsEncoder.getRelation(goldRels, ne1, ne2);
                String predLabel = RelationsEncoder.getRelation(predRels, ne1, ne2);
                
                if (predLabel.equals(goldLabel)) {
                    if (!goldLabel.equals(RelationsEncoder.NO_RELATION_LABEL)) {
                        numCorrectPositive++;
                    } else {
                        numCorrectNegative++;
                    }
                }
                if (!goldLabel.equals(RelationsEncoder.NO_RELATION_LABEL)) {
                    numTruePositive++;
                }
                if (!predLabel.equals(RelationsEncoder.NO_RELATION_LABEL)) {
                    numPredictPositive++;
                }
                numInstances++;
                log.trace(String.format("goldLabel=%s predLabel=%s", goldLabel, predLabel));                    
            }
        }
        precision = (double) numCorrectPositive / numPredictPositive;
        recall = (double) numCorrectPositive / numTruePositive;
        f1 = (double) (2 * precision * recall) / (precision + recall);
        
        log.info(String.format("Num relation instances on %s: %d", dataName, numInstances));
        log.info(String.format("Num true positives on %s: %d", dataName, numTruePositive));
        log.info(String.format("Precision on %s: %.4f", dataName, precision));
        log.info(String.format("Recall on %s: %.4f", dataName, recall));
        log.info(String.format("F1 on %s: %.4f", dataName, f1));
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
