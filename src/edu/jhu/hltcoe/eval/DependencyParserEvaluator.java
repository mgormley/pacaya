package edu.jhu.hltcoe.eval;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Computes the directed score (as described in Spitkovsky, 2009). Directed score
 * is simply the overall fraction of correctly guessed dependencies
 * 
 * @author mgormley
 */
public class DependencyParserEvaluator implements Evaluator {

    private static final Logger log = Logger.getLogger(DependencyParserEvaluator.class);

    private DepTreebank goldTreebank;
    private ViterbiParser parser;
    private double accuracy;
    private double logLikelihood;
    private String dataName;

    private double perTokenCrossEnt;

    private DepTreebank parses;

    public DependencyParserEvaluator(ViterbiParser parser, DepTreebank goldTreebank, String dataName) {
        this.parser = parser;
        this.goldTreebank = goldTreebank;
        this.dataName = dataName;
    }

    @Override
    public void evaluate(Model model) {
        SentenceCollection sentences = goldTreebank.getSentences();
        parses = parser.getViterbiParse(sentences, model);
        logLikelihood = parser.getLastParseWeight();
        perTokenCrossEnt = - logLikelihood / Utilities.log(2) / sentences.getNumTokens();
        
        evaluate(parses);
    }

    public double evaluate(DepTreebank parses) {
        int correct = 0;
        int total = 0;
        assert(parses.size() == goldTreebank.size());
        for (int i = 0; i < goldTreebank.size(); i++) {
            int[] goldParents = goldTreebank.get(i).getParents();
            int[] parseParents = parses.get(i).getParents();
            assert(parseParents.length == goldParents.length);
            for (int j = 0; j < goldParents.length; j++) {
                if (goldParents[j] == parseParents[j]) {
                    correct++;
                }
                total++;
            }
        }
        accuracy = (double) correct / (double) total;
        return accuracy;
    }

    @Override
    public void print() {
        log.info(String.format("Accuracy on %s: %.4f", dataName, accuracy));
        log.info(String.format("LogLikelihood on %s: %.4f", dataName, logLikelihood));
        log.info(String.format("Per token cross entropy on %s: %.3f", dataName, perTokenCrossEnt));
    }

    @Override
    public DepTreebank getParses() {
        return parses;
    }

}
