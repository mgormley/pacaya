package edu.jhu.hltcoe.eval;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.conll.CoNLL09DepTree;
import edu.jhu.hltcoe.data.conll.SrlGraph;
import edu.jhu.hltcoe.data.conll.SrlGraph.SrlArg;
import edu.jhu.hltcoe.data.conll.SrlGraph.SrlEdge;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.DepParser;

/**
 * Computes the proportion of words labeled as arguments that have a syntactic
 * head that agrees with one of its semantic heads.
 * 
 * @author mgormley
 */
public class SrlEdgeEvaluator implements Evaluator {

    private static final Logger log = Logger.getLogger(SrlEdgeEvaluator.class);

    private DepTreebank goldTreebank;
    private DepParser parser;
    private double accuracy;
    private String dataName;

    private DepTreebank parses;

    public SrlEdgeEvaluator(DepParser parser, DepTreebank goldTreebank, String dataName) {
        this.parser = parser;
        this.goldTreebank = goldTreebank;
        this.dataName = dataName;
    }

    @Override
    public void evaluate(Model model) {
        SentenceCollection sentences = goldTreebank.getSentences();
        parses = parser.getViterbiParse(sentences, model);        
        evaluate(parses);
    }

    public double evaluate(DepTreebank parses) {
        int correct = 0;
        int total = 0;
        assert(parses.size() == goldTreebank.size());
        for (int i = 0; i < goldTreebank.size(); i++) {
            int[] parseParents = parses.get(i).getParents();
            
            // Get a mapping from argument positions to SrlArg objects.
            CoNLL09DepTree conll09Tree = (CoNLL09DepTree) goldTreebank.get(i);
            SrlGraph srlGraph = conll09Tree.getCoNLL09Sentence().getSrlGraph();
            
            // For each word labeled as a semantic argument...
            for (SrlArg arg : srlGraph.getArgs()) {
                // Get the child's position.
                int argPosition = arg.getPosition();

                // Check whether the predicted parent agrees with one of the
                // predicates for this argument.
                for (SrlEdge edge : arg.getEdges()) {
                    if (parseParents[argPosition] == edge.getPred().getPosition()) {
                        correct++;
                        break;
                    }
                }
                
                total++;
            }
        }
        accuracy = (double) correct / (double) total;
        return accuracy;
    }
    
    @Override
    public void print() {
        log.info(String.format("Proportion semantically valid deps on %s: %.4f", dataName, accuracy));
    }

    @Override
    public DepTreebank getParses() {
        return parses;
    }

}
