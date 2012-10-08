package edu.jhu.hltcoe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import util.Alphabet;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreeNode;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.FileMapTagReducer;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Ptb45To17TagReducer;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.TaggedWord;
import edu.jhu.hltcoe.data.VerbTreeFilter;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.eval.Evaluator;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProjector;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolution;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDmvSolution;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer;
import edu.jhu.hltcoe.train.Trainer;
import edu.jhu.hltcoe.train.TrainerFactory;
import edu.jhu.hltcoe.train.LocalBnBDmvTrainer.InitSol;
import edu.jhu.hltcoe.util.Command;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;

public class PipelineRunner {

    private static Logger log = Logger.getLogger(PipelineRunner.class);

    public PipelineRunner() {
    }

    public void run(CommandLine cmd) throws ParseException, IOException {  
        
        // Get the training data
        DepTreebank trainTreebank;
        if (cmd.hasOption("train")) {
            // Read the data and (maybe) reduce size of treebank
            String trainPath = cmd.getOptionValue("train");
            log.info("Reading train data: " + trainPath);
            int maxSentenceLength = Command.getOptionValue(cmd, "maxSentenceLength", Integer.MAX_VALUE);
            Alphabet<Label> alphabet = new Alphabet<Label>();
            trainTreebank = getTreebank(cmd, trainPath, maxSentenceLength, alphabet);
        } else if (cmd.hasOption("synthetic")) {
            String synthetic = cmd.getOptionValue("synthetic");
            DmvModel trueModel;
            if (synthetic.equals("two")) {
                trueModel = SimpleStaticDmvModel.getTwoPosTagInstance();
            } else if (synthetic.equals("three")) {
                trueModel = SimpleStaticDmvModel.getThreePosTagInstance();
            } else {
                throw new ParseException("Unknown synthetic type: " + synthetic);
            }
            long syntheticSeed = 123454321;
            if (cmd.hasOption("syntheticSeed")) {
                syntheticSeed = Long.parseLong(cmd.getOptionValue("syntheticSeed"));
            }
            DmvDepTreeGenerator generator = new DmvDepTreeGenerator(trueModel, syntheticSeed);
            int maxNumSentences = Command.getOptionValue(cmd, "maxNumSentences", 100); 
            trainTreebank = generator.getTreebank(maxNumSentences);
        } else {
            throw new ParseException("Either the option --train or --synthetic must be specified");
        }
        
        // Divide into labeled and unlabeled data.
        double propSupervised = Command.getOptionValue(cmd, "propSupervised", 0.0);
        DmvTrainCorpus trainCorpus = new DmvTrainCorpus(trainTreebank, propSupervised); 
            
        log.info("Number of unlabeled train sentences: " + trainCorpus.getNumUnlabeled());
        log.info("Number of labeled train sentences: " + trainCorpus.getNumLabeled());
        log.info("Number of train sentences: " + trainTreebank.size());
        log.info("Number of train tokens: " + trainTreebank.getNumTokens());
        log.info("Number of train types: " + trainTreebank.getNumTypes());
                
        // Print train sentences to a file
        printSentences(cmd, trainTreebank);
        
        // Get the test data
        DepTreebank testTreebank = null;
        if (cmd.hasOption("test")) {
            // Read the data and (maybe) reduce size of treebank
            String testPath = cmd.getOptionValue("test");
            log.info("Reading test data: " + testPath);
            int maxSentenceLengthTest = Command.getOptionValue(cmd, "maxSentenceLengthTest", Integer.MAX_VALUE);
            
            testTreebank = getTreebank(cmd, testPath, maxSentenceLengthTest, trainTreebank.getAlphabet());

            log.info("Number of test sentences: " + testTreebank.size());
            log.info("Number of test tokens: " + testTreebank.getNumTokens());
            log.info("Number of test types: " + testTreebank.getNumTypes());
        }
        
        if (cmd.hasOption("relaxOnly")) {
            DmvRelaxation dw = (DmvRelaxation)TrainerFactory.getTrainer(cmd, trainTreebank); 
            dw.init1(trainCorpus);
            dw.init2(LocalBnBDmvTrainer.getInitSol(InitSol.UNIFORM, trainCorpus, null, null));
            DmvSolution initBoundsSol = updateBounds(cmd, trainCorpus, dw, trainTreebank);
            Stopwatch timer = new Stopwatch();
            timer.start();
            RelaxedDmvSolution relaxSol = dw.solveRelaxation();
            timer.stop();
            log.info("relaxTime(ms): " + Time.totMs(timer));
            log.info("relaxBound: " + relaxSol.getScore());
            if (initBoundsSol != null) {
                log.info("initBoundsSol: " + initBoundsSol.getScore());
                log.info("relative: " + Math.abs(relaxSol.getScore() - initBoundsSol.getScore()) / Math.abs(initBoundsSol.getScore()));
            }
            // TODO: use add-lambda smoothing here.
            DmvProjector dmvProjector = new DmvProjector(trainCorpus, dw);
            DmvSolution projSol = dmvProjector.getProjectedDmvSolution(relaxSol);
            log.info("projLogLikelihood: " + projSol.getScore());
            // TODO: Remove this hack. It's only to setup for getEvalParser().
            //            TrainerFactory.getTrainer(cmd, trainTreebank);
            //            DependencyParserEvaluator dpEval = new DependencyParserEvaluator(TrainerFactory.getEvalParser(), trainTreebank, "train");
            //            dpEval.evaluate(projSol)
            //            TODO: log.info("containsGoldSol: " + containsInitSol(dw.getBounds(), goldSol.getLogProbs()));
        } else {
            // Train the model
            log.info("Training model");
            Trainer trainer = (Trainer)TrainerFactory.getTrainer(cmd, trainTreebank);
            if (trainer instanceof BnBDmvTrainer) {
                BnBDmvTrainer bnb = (BnBDmvTrainer) trainer;
                bnb.init(trainCorpus);
                updateBounds(cmd, trainCorpus, bnb.getRootRelaxation(), trainTreebank);
                bnb.train();
            } else {
                trainer.train(trainCorpus);
            }
            Model model = trainer.getModel();
            
            // Evaluate the model on the training data
            log.info("Evaluating model on train");
            // Note: this parser must return the log-likelihood from parser.getParseWeight()
            ViterbiParser parser = TrainerFactory.getEvalParser();
            Evaluator trainEval = new DependencyParserEvaluator(parser, trainTreebank, "train");
            trainEval.evaluate(model);
            trainEval.print();
            
            // Evaluate the model on the test data
            if (testTreebank != null) {
                log.info("Evaluating model on test");
                Evaluator testEval = new DependencyParserEvaluator(parser, testTreebank, "test");
                testEval.evaluate(model);
                testEval.print();
            }
            
            // Print learned model to a file
            String printModel = Command.getOptionValue(cmd, "printModel", null);
            if (printModel != null) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(printModel));
                writer.write("Learned Model:\n");
                writer.write(model.toString());
                writer.close();
            }
        }
    }

    private DepTreebank getTreebank(CommandLine cmd, String trainPath, int maxSentenceLength, Alphabet<Label> alphabet) {
        DepTreebank trainTreebank;
        int maxNumSentences = Command.getOptionValue(cmd, "maxNumSentences", Integer.MAX_VALUE); 
        boolean mustContainVerb = cmd.hasOption("mustContainVerb");
        String reduceTags = Command.getOptionValue(cmd, "reduceTags", "none");

        // Create the original trainTreebank with a throw-away alphabet.
        trainTreebank = new DepTreebank(maxSentenceLength, maxNumSentences, new Alphabet<Label>());
        if (mustContainVerb) {
            trainTreebank.setTreeFilter(new VerbTreeFilter());
        }
        trainTreebank.loadPath(trainPath);
        
        if ("45to17".equals(reduceTags)) {
            log.info("Reducing PTB from 45 to 17 tags");
            (new Ptb45To17TagReducer()).reduceTags(trainTreebank);
        } else if (!"none".equals(reduceTags)) {
            log.info("Reducing tags with file map: " + reduceTags);
            (new FileMapTagReducer(new File(reduceTags))).reduceTags(trainTreebank);
        }

        // After reducing tags we create an entirely new treebank that uses the alphabet we care about.
        DepTreebank tmpTreebank = new DepTreebank(alphabet);
        for (DepTree tree : trainTreebank) {
            tmpTreebank.add(tree);
        }
        trainTreebank = tmpTreebank;
        return trainTreebank;
    }

    private DmvSolution updateBounds(CommandLine cmd, DmvTrainCorpus trainCorpus, DmvRelaxation dw, DepTreebank trainTreebank) {
        if (cmd.hasOption("initBounds")) {
            InitSol opt = InitSol.getById(Command.getOptionValue(cmd, "initBounds", "none"));
            double offsetProb = Command.getOptionValue(cmd, "offsetProb", 1.0);
            double probOfSkipCm = Command.getOptionValue(cmd, "probOfSkipCm", 0.0);
            int numDoubledCms = Command.getOptionValue(cmd, "numDoubledCms", 0);
            
            DmvSolution initBoundsSol = LocalBnBDmvTrainer.getInitSol(opt, trainCorpus, dw, trainTreebank);
            
            if (numDoubledCms > 0) {
                // TODO:
                throw new RuntimeException("not implemented");
            }
            
            LocalBnBDmvTrainer.setBoundsFromInitSol(dw, initBoundsSol, offsetProb, probOfSkipCm);
            
            return initBoundsSol;
        }
        return null;
    }

    private void printSentences(CommandLine cmd, DepTreebank depTreebank)
            throws IOException {
        SentenceCollection sentences = depTreebank.getSentences();
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
                log.info("Printing trees...");
                // Also print the synthetic trees
                writer.write("Trees:\n");
                writer.write(depTreebank.toString());
            }
            writer.close();
        }
    }

    public static Options createOptions() {
        Options options = new Options();
        
        // Options not specific to the model

        options.addOption("s", "seed", true, "Pseudo random number generator seed for everything else.");
        options.addOption("pm", "printModel", true, "File to which we should print the model.");
        options.addOption("ro", "relaxOnly", false, "Flag indicating that only a relaxation should be run");
        
        // Options for data
        options.addOption("tr", "train", true, "Training data.");
        options.addOption("tr", "synthetic", true, "Generate synthetic training data.");
        options.addOption("msl", "maxSentenceLength", true, "Max sentence length.");
        options.addOption("mns", "maxNumSentences", true, "Max number of sentences for training."); 
        options.addOption("vb", "mustContainVerb", false, "Filter down to sentences that contain certain verbs."); 
        options.addOption("rd", "reduceTags", true, "Tag reduction type [none, 45to17, {a file map}]."); 
        options.addOption("ps", "printSentences", true, "File to which we should print the sentences.");
        options.addOption("ss", "syntheticSeed", true, "Pseudo random number generator seed for synthetic data generation only.");
        options.addOption("psu", "propSupervised", true, "Proportion of labeled/supervised training data.");
        
        // Options for test data
        options.addOption("te", "test", true, "Test data.");
        options.addOption("mslt", "maxSentenceLengthTest", true, "Max sentence length for test data.");
        
        // Options to restrict the initialization
        options.addOption("ib", "initBounds", true, "How to initialize the bounds: [viterbi-em, gold, random, uniform, none]");
        
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
            log.error(e1.getMessage());
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
        for (String requiredOption : requiredOptions) {
            if (!cmd.hasOption(requiredOption)) {
                log.error("Missing required option: " + requiredOption);
                formatter.printHelp(usage, options, true);
                System.exit(1);
            }
        }
        
        Prng.seed(Command.getOptionValue(cmd, "seed", Prng.DEFAULT_SEED));
        
        PipelineRunner pipeline = new PipelineRunner();
        try {
            pipeline.run(cmd);
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            formatter.printHelp(usage, options, true);
            System.exit(1);
        }
    }

}
