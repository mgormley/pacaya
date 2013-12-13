package edu.jhu.eval;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.model.Model;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.prim.util.math.FastMath;

/**
 * Computes the unlabeled directed dependency accuracy. This is simply the
 * overall fraction of correctly predicted dependencies.
 * 
 * @author mgormley
 */
public class DependencyParserEvaluator {

    private static final Logger log = Logger.getLogger(DependencyParserEvaluator.class);

    private DepTreebank goldTreebank;
    private DepParser parser;
    private double accuracy;
    private double logLikelihood;
    private String dataName;

    private double perTokenCrossEnt;

    private DepTreebank parses;

    public DependencyParserEvaluator(DepParser parser, DepTreebank goldTreebank, String dataName) {
        this.parser = parser;
        this.goldTreebank = goldTreebank;
        this.dataName = dataName;
    }

    public void evaluate(Model model) {
        SentenceCollection sentences = goldTreebank.getSentences();
        parses = parser.getViterbiParse(sentences, model);
        logLikelihood = parser.getLastParseWeight();
        perTokenCrossEnt = - logLikelihood / FastMath.log(2) / sentences.getNumTokens();
        
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
        
        log.info(String.format("Accuracy on %s: %.4f", dataName, accuracy));
        return accuracy;
    }

}
