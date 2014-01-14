package edu.jhu.induce.train.dmv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.DepTreebankReader;
import edu.jhu.data.DepTreebankReader.DatasetType;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.TaggedWord;
import edu.jhu.data.conll.CoNLL09DepTree;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.CoNLLXDepTree;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXWriter;
import edu.jhu.eval.DependencyParserEvaluator;
import edu.jhu.eval.SrlEdgeEvaluator;
import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvProjector;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvRelaxedSolution;
import edu.jhu.globalopt.dmv.DmvSolFactory;
import edu.jhu.globalopt.dmv.DmvSolution;
import edu.jhu.globalopt.dmv.IndexedDmvModel;
import edu.jhu.globalopt.dmv.BasicDmvProjector.DmvProjectorFactory;
import edu.jhu.globalopt.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.globalopt.dmv.DmvSolFactory.InitSol;
import edu.jhu.induce.model.dmv.DmvDepTreeGenerator;
import edu.jhu.induce.model.dmv.DmvModel;
import edu.jhu.induce.model.dmv.SimpleStaticDmvModel;
import edu.jhu.induce.train.Trainer;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.files.Files;

public class DepParserRunner {
    
    private static final Logger log = Logger.getLogger(DepParserRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(name = "relaxOnly", hasArg = true, description = "Flag indicating that only a relaxation should be run")
    public static boolean relaxOnly = false;

    // Options for train data
    @Opt(name = "train", hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(name = "trainType", hasArg = true, required = true, description = "Type of training data.")
    public static DatasetType trainType = null;
    @Opt(name = "trainOut", hasArg = true, description = "Training data output file.")
    public static File trainOut = null;
    @Opt(name = "synthetic", hasArg = true, description = "Which type of synthetic training data to generate.")
    public static String synthetic = null;
    @Opt(name = "printSentences", hasArg = true, description = "File to which we should print the sentences.")
    public static File printSentences = null;
    @Opt(name = "syntheticSeed", hasArg = true, description = "Pseudo random number generator seed for synthetic data generation only.")
    public static long syntheticSeed = 123454321;
    @Opt(name = "propSupervised", hasArg = true, description = "Proportion of labeled/supervised training data.")
    public static double propSupervised = 0.0;

    // Options for test data
    @Opt(name = "test", hasArg = true, description = "Testing data input file or directory.")
    public static File test = null;
    @Opt(name = "testType", hasArg = true, description = "Type of testing data.")
    public static DatasetType testType = null;
    @Opt(name = "testOut", hasArg = true, description = "Testing data output file.")
    public static File testOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for test data.")
    public static int maxSentenceLengthTest = Integer.MAX_VALUE;

    // Options for model IO
    @Opt(hasArg = true, description = "File from which we should read a serialized model.")
    public static File modelIn = null;
    @Opt(hasArg = true, description = "File to which we should serialize the model.")
    public static File modelOut = null;
    @Opt(hasArg = true, description = "File to which we should print a human readable version of the model.")
    public static File printModel = null;
    
    // Options to restrict the initialization
    @Opt(name = "initBounds", hasArg = true, description = "How to initialize the bounds. [VITERBI_EM, GOLD, RANDOM, UNIFORM, SUPERVISED, NONE]")
    public static InitSol initBounds = null;

    public DepParserRunner() {
    }

    public void run() throws ParseException, IOException {  
        // Get a model.
        DmvModel model = null;
        Alphabet<Label> alphabet;
        DmvModel goldModel = null;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (DmvModel) Files.deserialize(modelIn);
            alphabet = model.getTagAlphabet();
        } else {
            alphabet = new Alphabet<Label>();
        }
        
        if (trainType != null && train != null) {
            if (model != null) {
                log.error("Initializing training from an initial model is currently not supported.");
                throw new ParseException("Initializing training from an initial model is currently not supported. "
                        + "Must specify only one of --train and --modelIn.");
            }
            
            // Train a model.
            DepTreebank trainTreebank = getData(alphabet, trainType, train, DepTreebankReader.maxSentenceLength, "train");
            
            // Divide into labeled and unlabeled data.
            DmvTrainCorpus trainCorpus = new DmvTrainCorpus(trainTreebank, propSupervised);             
            log.info("Number of unlabeled train sentences: " + trainCorpus.getNumUnlabeled());
            log.info("Number of labeled train sentences: " + trainCorpus.getNumLabeled());
            
            // Print train sentences to a file
            if (printSentences != null) {
                printSentences(trainCorpus.getSentences(), printSentences);
            }
            
            if (relaxOnly) {
                // Just run the relaxation on the training data. This is a
                // special case: it produces no model, does no actual training,
                // and the only output is what's written to the log.
                runRelaxOnly(goldModel, trainTreebank, trainCorpus);
                // Return and exit this pipeline.
                return;
            }
                    
            // Train the model
            log.info("Training model");
            Trainer trainer = DmvTrainerFactory.getTrainer(trainTreebank, goldModel);
            if (trainer instanceof BnBDmvTrainer) {
                BnBDmvTrainer bnb = (BnBDmvTrainer) trainer;
                bnb.init(trainCorpus);
                updateBounds(trainCorpus, bnb.getRootRelaxation(), trainTreebank, goldModel);
                bnb.train();
            } else {
                trainer.train(trainCorpus);
            }
            model = (DmvModel) trainer.getModel();

            // Parse and evaluate the test data.
            evalAndWrite(model, trainTreebank, "train", trainOut, trainType);            
        }
        
        if (modelOut != null) {
            // Write the model to a file.
            log.info("Serializing model to file: " + modelOut);
            Files.serialize(model, modelOut);
        }
        
        if (printModel != null) {
            // Print learned model to a file
            log.info("Printing human readable model to file: " + printModel);
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(printModel), "UTF-8"));
            writer.write("Learned Model:\n");
            writer.write(model.toString());
            writer.close();
        }

        if (test != null && testType != null) {
            // Test the model on test data.
            alphabet.stopGrowth();
            
            // Read the data and (maybe) reduce size of treebank
            log.info("Reading test data: " + test);
            DepTreebank testTreebank = getData(alphabet, testType, test, maxSentenceLengthTest, "test");

            // Parse and evaluate the test data.
            evalAndWrite(model, testTreebank, "test", testOut, testType);
        }
    }

    private void runRelaxOnly(DmvModel goldModel, DepTreebank trainTreebank, DmvTrainCorpus trainCorpus)
            throws ParseException {
        DmvSolFactory initSolFactory = new DmvSolFactory(DmvTrainerFactory.getDmvSolFactoryPrm(trainTreebank, goldModel));
        DmvSolution initSol = initSolFactory.getInitFeasSol(trainCorpus);
        DmvRelaxationFactory relaxFactory = DmvTrainerFactory.getDmvRelaxationFactory();
        DmvRelaxation relax = relaxFactory.getInstance(trainCorpus, initSol);
        DmvSolution initBoundsSol = updateBounds(trainCorpus, relax, trainTreebank, goldModel);
        Timer timer = new Timer();
        timer.start();
        DmvProblemNode rootNode = new DmvProblemNode(null);
        DmvRelaxedSolution relaxSol = (DmvRelaxedSolution) relax.getRelaxedSolution(rootNode);
        timer.stop();
        log.info("relaxTime(ms): " + timer.totMs());
        log.info("relaxBound: " + relaxSol.getScore());
        if (initBoundsSol != null) {
            log.info("initBoundsSol: " + initBoundsSol.getScore());
            log.info("relative: " + Math.abs(relaxSol.getScore() - initBoundsSol.getScore()) / Math.abs(initBoundsSol.getScore()));
        }
        DmvProjectorFactory projectorFactory = DmvTrainerFactory.getDmvProjectorFactory(trainTreebank, goldModel);
        DmvProjector dmvProjector = (DmvProjector) projectorFactory.getInstance(trainCorpus, relax);
        DmvSolution projSol = dmvProjector.getProjectedDmvSolution(relaxSol);
        if (projSol != null) {
            log.info("projLogLikelihood: " + projSol.getScore());
        } else {
            log.warn("projLogLikelihood: UNAVAILABLE");
        }
    }
    
    private DepTreebank getData(Alphabet<Label> alphabet, DatasetType dataType, File dataFile, int maxSentenceLength, String dataName) throws ParseException, IOException {
        // Get the training data
        DepTreebank goldTreebank;
        DmvModel goldModel = null;
        if (dataType == DatasetType.PTB || dataType == DatasetType.CONLL_X
                || dataType == DatasetType.CONLL_2009) {
            // Read the data and (maybe) reduce size of treebank
            log.info("Reading " + dataName + " data: " + dataFile);
            goldTreebank = DepTreebankReader.getTreebank(dataFile, dataType, maxSentenceLength, alphabet);
        } else if (dataType == DatasetType.SYNTHETIC) {
            if (synthetic == null) {
                throw new ParseException("--synthetic must be specified");
            }
            if (synthetic.equals("two")) {
                goldModel = SimpleStaticDmvModel.getTwoPosTagInstance();
            } else if (synthetic.equals("three")) {
                goldModel = SimpleStaticDmvModel.getThreePosTagInstance();
            } else if (synthetic.equals("alt-three")) {
                goldModel = SimpleStaticDmvModel.getAltThreePosTagInstance();
            } else {
                throw new ParseException("Unknown synthetic type: " + synthetic);
            }
            DmvDepTreeGenerator generator = new DmvDepTreeGenerator(goldModel, syntheticSeed);
            goldTreebank = generator.getTreebank(DepTreebankReader.maxNumSentences);
        } else {
            throw new ParseException("Either the option --train or --synthetic must be specified");
        }
        
        log.info("Number of " + dataName + " sentences: " + goldTreebank.size());
        log.info("Number of " + dataName + " tokens: " + goldTreebank.getNumTokens());
        log.info("Number of " + dataName + " types: " + goldTreebank.getNumTypes());
        
        return goldTreebank;
    }

    private void evalAndWrite(DmvModel model, DepTreebank goldTreebank, String datasetName, File trainOut, DatasetType trainType) throws IOException {
        // TODO: This does not correctly evaluate semi-supervised datasets and instead treats them as entirely unsupervised. 
        
        // Evaluation.
        // Note: this parser must return the log-likelihood from parser.getParseWeight()
        DepParser parser = DmvTrainerFactory.getEvalParser();

        SentenceCollection sentences = goldTreebank.getSentences();
        DepTreebank parses = parser.getViterbiParse(sentences, model);
        double logLikelihood = parser.getLastParseWeight();
        double perTokenCrossEnt = - logLikelihood / FastMath.log(2) / sentences.getNumTokens();
        log.info(String.format("LogLikelihood on %s: %.4f", datasetName, logLikelihood));
        log.info(String.format("Per token cross entropy on %s: %.3f", datasetName, perTokenCrossEnt));
        
        if (trainOut != null) {
            // Write parses to a file.
            writeParses(parses, trainOut, trainType, goldTreebank);
        }
        
        log.info("Evaluating model on " + datasetName);
        // Evaluate syntactic dependency accuracy.
        DependencyParserEvaluator synDepEval = new DependencyParserEvaluator(parser, goldTreebank, datasetName);
        synDepEval.evaluate(parses);
        
        if (trainType == DatasetType.CONLL_2009) {
            // Evaluate proportion of syntactic dependencies that agree with the semantic dependencies.
            SrlEdgeEvaluator semDepEval = new SrlEdgeEvaluator(parser, goldTreebank, datasetName);            
            semDepEval.evaluate(parses);
        }
    }

    /** 
     * Writes the gives parses to a file of the specified type.
     * 
     * @param parses The parses to write.
     * @param trainOut The output file.
     * @param trainType The type of output file.
     * @param goldTreebank The gold treebank which carries some additional annotations we may want to keep.
     */
    private static void writeParses(DepTreebank parses, File trainOut, DatasetType trainType, DepTreebank goldTreebank)
            throws IOException {
        // Write the parses of the training data out to a file.
        if (trainType == DatasetType.CONLL_X) {
            CoNLLXWriter cw = new CoNLLXWriter(trainOut);
            for (int i=0; i<parses.size(); i++) {
                // Make a copy of the original CoNLL-X sentence.
                DepTree goldTree = goldTreebank.get(i);
                CoNLLXSentence sent = ((CoNLLXDepTree)goldTree).getCoNLLXSentence();
                sent = new CoNLLXSentence(sent);
                // Update the parents in the copy.
                DepTree parse = parses.get(i);
                sent.setHeadsFromParents(parse.getParents());
                // Write the sentence.
                cw.write(sent);
            }
            cw.close();
        } else if (trainType == DatasetType.CONLL_2009) {
            CoNLL09Writer cw = new CoNLL09Writer(trainOut);
            for (int i=0; i<parses.size(); i++) {
                // Make a copy of the original CoNLL-09 sentence.
                DepTree goldTree = goldTreebank.get(i);
                CoNLL09Sentence sent = ((CoNLL09DepTree)goldTree).getCoNLL09Sentence();
                sent = new CoNLL09Sentence(sent);
                // Update the parents in the copy.
                DepTree parse = parses.get(i);
                sent.setPheadsFromParents(parse.getParents());
                // Write the sentence.
                cw.write(sent);
            }
            cw.close();
        } else {
            throw new RuntimeException("Unhandled dataset type: " + trainType);
        }
    }

    // TODO: This should update the deltas of the root node.
    private DmvSolution updateBounds(DmvTrainCorpus trainCorpus, DmvRelaxation dw, DepTreebank trainTreebank, DmvModel trueModel) throws ParseException {
        if (initBounds != null) {
            // Initialize the bounds as a hypercube around some initial solution.
            double offsetProb = DmvTrainerFactory.offsetProb;
            double probOfSkipCm = DmvTrainerFactory.probOfSkipCm;
            
            DmvSolution goldSol = null;
            if (trueModel != null) { 
                IndexedDmvModel idm = new IndexedDmvModel(trainCorpus);
                goldSol = new DmvSolution(idm.getCmLogProbs(trueModel), idm, trainTreebank, Double.NaN);
            }
            // TODO: replace this with an TrainerFactory.getInitSolFactoryPrm();
            DmvSolution initBoundsSol = DmvSolFactory.getInitSol(initBounds, trainCorpus, dw, trainTreebank, goldSol);
            
            LocalBnBDmvTrainer.setBoundsFromInitSol(dw, initBoundsSol, offsetProb, probOfSkipCm);
            
            return initBoundsSol;
        }
        return null;
    }

    private void printSentences(SentenceCollection sentences, File printSentences) 
            throws IOException {
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
        writer.close();
    }
    
    private static void configureLogging() {
        //ConsoleAppender cAppender = new ConsoleAppender(new EnhancedPatternLayout("%d{HH:mm:ss,SSS} [%t] %p %c %x - %m%n"));
        //BasicConfigurator.configure(cAppender); 
    }
    
    public static void main(String[] args) throws IOException {
        configureLogging();
        
        ArgParser parser = new ArgParser(DepParserRunner.class);
        parser.addClass(DepParserRunner.class);
        parser.addClass(DepTreebankReader.class);
        parser.addClass(DmvTrainerFactory.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Prng.seed(seed);
        
        DepParserRunner pipeline = new DepParserRunner();
        try {
            pipeline.run();
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            parser.printUsage();
            System.exit(1);
        }
    }

}
