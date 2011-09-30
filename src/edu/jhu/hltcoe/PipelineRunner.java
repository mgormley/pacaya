package edu.jhu.hltcoe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.FileMapTagReducer;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Ptb45To17TagReducer;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.data.VerbTreeFilter;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.eval.Evaluator;
import edu.jhu.hltcoe.model.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.SimpleStaticDmvModel;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.Trainer;
import edu.jhu.hltcoe.train.TrainerFactory;
import edu.jhu.hltcoe.util.Command;

public class PipelineRunner {

    private static Logger log = Logger.getLogger(PipelineRunner.class);

    public PipelineRunner() {
    }

    public void run(CommandLine cmd) throws ParseException, IOException {  
        DepTreebank depTreebank;
        if (cmd.hasOption("train")) {
            // Read the data and (maybe) reduce size of treebank
            log.info("Reading data");
            String trainPath = cmd.getOptionValue("train");
            int maxSentenceLength = Command.getOptionValue(cmd, "maxSentenceLength", Integer.MAX_VALUE);
            int maxNumSentences = Command.getOptionValue(cmd, "maxNumSentences", Integer.MAX_VALUE); 
    
            depTreebank = new DepTreebank(maxSentenceLength, maxNumSentences);
            if (cmd.hasOption("mustContainVerb")) {
                depTreebank.setTreeFilter(new VerbTreeFilter());
            }
            depTreebank.loadPath(trainPath);
            
            String reduceTags = Command.getOptionValue(cmd, "reduceTags", "none");
            if ("45to17".equals(reduceTags)) {
                log.info("Reducing PTB from 45 to 17 tags");
                (new Ptb45To17TagReducer()).reduceTags(depTreebank);
            } else if (!"none".equals(reduceTags)) {
                log.info("Reducing tags with file map: " + reduceTags);
                (new FileMapTagReducer(new File(reduceTags))).reduceTags(depTreebank);
            }
        } else if (cmd.hasOption("synthetic")) {
            DmvModel trueModel = SimpleStaticDmvModel.getSimplestInstance();
            DmvDepTreeGenerator generator = new DmvDepTreeGenerator(trueModel);
            int maxNumSentences = Command.getOptionValue(cmd, "maxNumSentences", 100); 
            depTreebank = generator.getTreebank(maxNumSentences);
        } else {
            throw new ParseException("Either the option --train or --synthetic must be specific");
        }
        
        log.info("Number of sentences: " + depTreebank.size());
        log.info("Number of tokens: " + depTreebank.getNumTokens());
        log.info("Number of types: " + depTreebank.getNumTypes());
        
        SentenceCollection sentences = depTreebank.getSentences();
        
        // Print sentences to a file
        String printSentences = Command.getOptionValue(cmd, "printSentences", null);
        if (printSentences != null) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(printSentences));
            // TODO: improve this
            log.info("Printing sentences...");
            writer.write("Sentences:\n");
            for (Sentence sent : sentences) {
                StringBuilder sb = new StringBuilder();
                for (Label label : sent) {
                    if (label instanceof TaggedWord) {
                        sb.append(((TaggedWord)label).getWord());
                    } else {
                        sb.append(label.getLabel());
                    }
                    sb.append(" ");
                }
                sb.append("\t");
                for (Label label : sent) {
                    if (label instanceof TaggedWord) {
                        sb.append(((TaggedWord)label).getTag());
                        sb.append(" ");
                    }
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
            if (cmd.hasOption("synthetic")) {
                log.info("Print trees...");
                // Also print the synthetic trees
                writer.write("Trees:\n");
                writer.write(depTreebank.toString());
            }
            writer.close();
        }
        
        // Train the model
        log.info("Training model");
        Trainer trainer = TrainerFactory.getTrainer(cmd);
        trainer.train(sentences);
        Model model = trainer.getModel();
        
        // Evaluate the model
        log.info("Evaluating model");
        // Note: this parser must return the log-likelihood from parser.getParseWeight()
        ViterbiParser parser = TrainerFactory.getEvalParser();
        Evaluator pwEval = new DependencyParserEvaluator(parser, depTreebank);
        pwEval.evaluate(model);
        pwEval.print();
        
        // Print learned model to a file
        String printModel = Command.getOptionValue(cmd, "printModel", null);
        if (printModel != null) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(printModel));
            writer.write("Learned Model:\n");
            writer.write(model.toString());
            writer.close();
        }
    }

    public static Options createOptions() {
        Options options = new Options();
        
        // Options not specific to the model
        options.addOption("tr", "train", true, "Training data.");
        options.addOption("tr", "synthetic", true, "Generate synthetic training data.");
        options.addOption("msl", "maxSentenceLength", true, "Max sentence length.");
        options.addOption("mns", "maxNumSentences", true, "Max number of sentences for training."); 
        options.addOption("vb", "mustContainVerb", false, "Filter down to sentences that contain certain verbs."); 
        options.addOption("rd", "reduceTags", true, "Tag reduction type [none, 45to17, {a file map}]."); 
        options.addOption("ps", "printSentences", true, "File to which we should print the sentences.");
        options.addOption("pm", "printModel", true, "File to which we should print the model.");
        
        TrainerFactory.addOptions(options);
        return options;
    }

    private static void configureLogging() {
        BasicConfigurator.configure();
    }
    
    public static void main(String[] args) throws IOException {
        configureLogging();
        
        String usage = "java " + PipelineRunner.class.getName() + " [OPTIONS]";
        CommandLineParser parser = new PosixParser();
        Options options = createOptions();
        String[] requiredOptions = new String[] { };

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
