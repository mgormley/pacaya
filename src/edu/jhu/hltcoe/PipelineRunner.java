package edu.jhu.hltcoe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.eval.Evaluator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.Trainer;
import edu.jhu.hltcoe.train.TrainerFactory;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeVisitor;
import edu.stanford.nlp.trees.Treebank;

public class PipelineRunner {

    private static Logger log = Logger.getLogger(PipelineRunner.class);

    public PipelineRunner() {
    }

    public void run(CommandLine cmd) throws ParseException {        
        // Read the data
        log.info("Reading data");
        String trainPath = cmd.getOptionValue("train");
        Treebank treebank = new MemoryTreebank();
        CategoryWordTag.suppressTerminalDetails = true;
        treebank.loadPath(trainPath);
        final HeadFinder chf = new CollinsHeadFinder();
        treebank.apply(new TreeVisitor() {
          public void visitTree(Tree pt) {
            pt.percolateHeads(chf);
          }
        });
        
        // Reduce size of treebank
        DepTreebank depTreebank;
        if (cmd.hasOption("maxSentenceLength") || cmd.hasOption("maxNumSentences")) {
            DepTreebank depTreebankTmp = new DepTreebank(treebank);
            depTreebank = new DepTreebank();
            int maxSentenceLength = Integer.MAX_VALUE;
            if (cmd.hasOption("maxSentenceLength")) {
                maxSentenceLength = Integer.valueOf(cmd.getOptionValue("maxSentenceLength"));
            }
            int maxNumSentences = Integer.MAX_VALUE;
            if (cmd.hasOption("maxNumSentences")) {
                maxNumSentences = Integer.valueOf(cmd.getOptionValue("maxNumSentences"));
            }
            for (DepTree tree : depTreebankTmp) {
                if (depTreebank.size() >= maxNumSentences) {
                    break;
                }
                int len = tree.getNumWords();
                if (len <= maxSentenceLength) {
                    depTreebank.add(tree);
                }
            }
        } else {
            depTreebank = new DepTreebank(treebank);
        }
        log.info("Number of sentences: " + depTreebank.size());
        log.info("Number of words: " + depTreebank.getNumWords());
        
        SentenceCollection sentences = depTreebank.getSentences();

        // Train the model
        log.info("Training model");
        Trainer trainer = TrainerFactory.getModel(cmd);
        trainer.train(sentences);
        ViterbiParser parser = ((ViterbiTrainer)trainer).getParser();
        Model model = trainer.getModel();
        
        // Evaluate the model
        log.info("Evaluating model");
        Evaluator pwEval = new DependencyParserEvaluator(parser, depTreebank);
        pwEval.evaluate(model);
        pwEval.print();
    }

    public static Options createOptions() {
        Options options = new Options();
        
        // Options not specific to the model
        options.addOption("tr", "train", true, "Training data.");
        options.addOption("msl", "maxSentenceLength", true, "Max sentence length.");
        options.addOption("mns", "maxNumSentences", true, "Max number of sentences for training.");

        TrainerFactory.addOptions(options);
        return options;
    }

    private static void configureLogging() {
        BasicConfigurator.configure();
    }
    
    public static void main(String[] args) {
        configureLogging();
        
        String usage = "java " + PipelineRunner.class.getName() + " [OPTIONS]";
        CommandLineParser parser = new PosixParser();
        Options options = createOptions();
        String[] requiredOptions = new String[] { "train" };

        CommandLine cmd = null;
        final HelpFormatter formatter = new HelpFormatter();
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
        for (String requiredOption : requiredOptions) {
            if (!cmd.hasOption(requiredOption)) {
                formatter.printHelp(usage, options, true);
                System.exit(1);
            }
        }

        PipelineRunner pipeline = new PipelineRunner();
        try {
            pipeline.run(cmd);
        } catch (ParseException e1) {
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
    }

}
