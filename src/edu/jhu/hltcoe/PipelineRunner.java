package edu.jhu.hltcoe;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.eval.Evaluator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.stanford.nlp.trees.MemoryTreebank;
import edu.stanford.nlp.ling.CategoryWordTag;
import edu.stanford.nlp.trees.CollinsHeadFinder;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeVisitor;
import edu.stanford.nlp.trees.Treebank;

public class PipelineRunner {

    public PipelineRunner() {
        // TODO:
    }

    public void run(CommandLine cmd) throws ParseException {
        // Read the data
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

        // Train the model
        Model model = ModelFactory.getModel(cmd);
        model.train(treebank);

        // Evaluate the model
        PrintWriter pw = new PrintWriter(System.out);
        if (cmd.hasOption("psuedo-word")) {
            Evaluator pwEval = new DependencyParserEvaluator();
            pwEval.evaluate(model);
            pwEval.print(pw);
        }
    }

    public static Options createOptions() {
        Options options = new Options();
        
        // Options not specific to the model
        options.addOption("tr", "train", true, "Training data.");

        ModelFactory.addOptions(options);
        return options;
    }

    public static void main(String[] args) {
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
