package edu.jhu.nlp.eval;

import org.apache.log4j.Logger;

import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public class SrlSelfLoops implements Evaluator {
    
    private static final Logger log = Logger.getLogger(SrlSelfLoops.class);

    @Override
    public void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents, String name) {
        printPredArgSelfLoopStats(goldSents, "gold " + name);
    }
    
    private static void printPredArgSelfLoopStats(AnnoSentenceCollection sents, String name) {
        int numPredArgSelfLoop = 0;
        int numPredArgs = 0;
        for (AnnoSentence sent : sents) {
            if (sent.getSrlGraph() != null) {
                for (SrlEdge edge : sent.getSrlGraph().getEdges()) {
                    if (edge.getArg().getPosition() == edge.getPred().getPosition()) {
                        numPredArgSelfLoop += 1;
                    }
                }
                numPredArgs += sent.getSrlGraph().getEdges().size();
            }
        }
        log.info(String.format("Proportion pred-arg self loops on %s: %.4f (%d / %d)", name, (double) numPredArgSelfLoop/numPredArgs, numPredArgSelfLoop, numPredArgs));
    }

}
