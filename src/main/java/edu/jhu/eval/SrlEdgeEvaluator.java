package edu.jhu.eval;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.conll.CoNLL09DepTree;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.convert.Ptb2ConllX;
import edu.jhu.data.deptree.DepTreebankReader;
import edu.jhu.data.deptree.DepTreebankReader.DatasetType;
import edu.jhu.induce.model.Model;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Computes the proportion of words labeled as arguments that have a syntactic
 * head that agrees with one of its semantic heads.
 * 
 * @author mgormley
 */
public class SrlEdgeEvaluator {

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
        log.info(String.format("Proportion semantically valid deps on %s: %.4f", dataName, accuracy));
        return accuracy;
    }
    

    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, required = true, description = "Type of training data.")
    public static DatasetType trainType = null;
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(Ptb2ConllX.class);
        parser.addClass(SrlEdgeEvaluator.class);
        parser.addClass(DepTreebankReader.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Alphabet<Label> alphabet = new Alphabet<Label>();
        DepTreebank goldTreebank = DepTreebankReader.getTreebank(train, trainType, alphabet);
        SrlEdgeEvaluator eval = new SrlEdgeEvaluator(null, goldTreebank, "data");
        eval.evaluate(goldTreebank);
    }
    
}
