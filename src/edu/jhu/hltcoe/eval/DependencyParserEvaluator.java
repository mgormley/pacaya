package edu.jhu.hltcoe.eval;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.ViterbiParser;

/**
 * Computes the directed score (as described in Spitkovsky, 2009). Directed score
 * is simply the overall fraction of cor- rectly guessed dependencies
 * 
 * @author mgormley
 */
public class DependencyParserEvaluator implements Evaluator {

    private static Logger log = Logger.getLogger(DependencyParserEvaluator.class);

    private DepTreebank depTreebank;
    private ViterbiParser parser;
    private double accuracy;

    public DependencyParserEvaluator(ViterbiParser parser, DepTreebank depTreebank) {
        this.parser = parser;
        this.depTreebank = depTreebank;
    }

    @Override
    public void evaluate(Model model) {
        int correct = 0;
        int total = 0;
        SentenceCollection sentences = depTreebank.getSentences();
        DepTreebank parses = parser.getViterbiParse(sentences, model);
        for (int i = 0; i < depTreebank.size(); i++) {
            int[] goldParents = depTreebank.get(i).getParents();
            int[] parseParents = parses.get(i).getParents();
            for (int j = 0; j < goldParents.length; j++) {
                if (goldParents[j] == parseParents[j]) {
                    correct++;
                }
                total++;
            }
        }
        accuracy = (double) correct / (double) total;
    }

    @Override
    public void print() {
        log.info(String.format("Accuracy: %.2f", accuracy));
    }

}
