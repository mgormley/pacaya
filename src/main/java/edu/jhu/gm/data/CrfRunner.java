package edu.jhu.gm.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.gm.AccuracyEvaluator;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfTrainer;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.MbrDecoder;
import edu.jhu.gm.MbrDecoder.Loss;
import edu.jhu.gm.MbrDecoder.MbrDecoderPrm;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Files;
import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.dist.Gaussian;

public class CrfRunner {

    public static enum DatasetType { ERMA };

    public static enum InitParams { UNIFORM, RANDOM };
    
    private static final Logger log = Logger.getLogger(CrfRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;

    // Options for train data
    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, description = "Type of training data.")
    public static DatasetType trainType = DatasetType.ERMA;
    @Opt(hasArg = true, description = "ERMA feature file.")
    public static File featureFileIn = null;
    @Opt(hasArg = true, description = "Training data predictions output file.")
    public static File trainPredOut = null;

    // Options for test data
    @Opt(hasArg = true, description = "Testing data input file or directory.")
    public static File test = null;
    @Opt(hasArg = true, description = "Type of testing data.")
    public static DatasetType testType = DatasetType.ERMA;
    @Opt(hasArg = true, description = "Testing data predictions output file.")
    public static File testPredOut = null;

    // Options for model IO
    @Opt(hasArg = true, description = "File from which we should read a serialized model.")
    public static String modelIn = null;
    @Opt(hasArg = true, description = "File to which we should serialize the model.")
    public static String modelOut = null;
    @Opt(hasArg = true, description = "File to which we should print a human readable version of the model.")
    public static String printModel = null;

    // Options for initialization.
    @Opt(hasArg = true, description = "How to initialize the parameters of the model.")
    public static InitParams initParams = InitParams.UNIFORM;
    
    // Options for inference.
    @Opt(hasArg = true, description = "Whether to run inference in the log-domain.")
    public static boolean logDomain = true;
    
    
    public CrfRunner() {
    }

    public void run() throws ParseException, IOException {  
        if (logDomain) {
            Utilities.useLogAddTable = true;
        }
        
        // Get a model.
        FgModel model = null;
        Alphabet<Feature> alphabet;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (FgModel) Files.deserialize(modelIn);
            alphabet = model.getAlphabet();
        } else {
            alphabet = new Alphabet<Feature>();
        }
        
        if (trainType != null && train != null) {
            assert(trainType == DatasetType.ERMA);
            // Train a model.
            // TODO: add option for useUnsupportedFeatures.
            ErmaReader er = new ErmaReader(true);
            er.read(featureFileIn, train);
            log.info("Converting ERMA objects to our objects.");
            FgExamples data = er.getDataExs(alphabet);  
            er = null; // Allow for GC.
            
            log.info("Num examples in train: " + data.size());
            log.info("Num factors in train: " + data.getNumFactors());
            log.info("Num variables in train: " + data.getNumVars());
            log.info("Num features: " + data.getAlphabet().size());
                        
            if (model == null) {
                model = new FgModel(alphabet);
                if (initParams == InitParams.RANDOM) {
                    // Fill the model parameters will values randomly drawn from ~ Normal(0, 1).
                    Gaussian.nextDoubleArray(0.0, 1.0, model.getParams());
                } else if (initParams == InitParams.UNIFORM) {
                    // Do nothing.
                } else {
                    throw new ParseException("Parameter initialization method not implemented: " + initParams);
                }
            } else {
                log.info("Using read model as initial parameters for training.");
            }
            
            log.info("Training model.");
            CrfTrainerPrm prm = getCrfTrainerPrm();
            CrfTrainer trainer = new CrfTrainer(prm);
            model = trainer.train(model, data);
            trainer = null; // Allow for GC.
            
            // Decode the training data.
            log.info("Running the decoder on training data.");
            MbrDecoder decoder = getDecoder();
            decoder.decode(model, data);
            
            AccuracyEvaluator accEval = new AccuracyEvaluator();
            double accuracy = accEval.evaluate(data.getGoldConfigs(), decoder.getMbrVarConfigList());
            log.info("Train accuracy: " + accuracy);
            
            if (trainPredOut != null) {
                ErmaWriter ew = new ErmaWriter();
                ew.writePredictions(trainPredOut, decoder.getMbrVarConfigList(), decoder.getVarMargMap());
            }
        } else {
            throw new ParseException("Either train/trainType or modelIn must be specified.");
        }
        
        if (modelOut != null) {
            // Write the model to a file.
            log.info("Serializing model to file: " + modelOut);
            Files.serialize(model, modelOut);
        }
        if (printModel != null) {
            // Print the model to a file.
            log.info("Printing human readable model to file: " + printModel);
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(printModel), "UTF-8"));
            model.printModel(writer);
            writer.close();
        }

        if (test != null && testType != null) {
            alphabet.stopGrowth();
            
            assert(testType == DatasetType.ERMA);
            // Test the model on test data.
            ErmaReader er = new ErmaReader(true);
            er.read(featureFileIn, train);
            FgExamples data = er.getDataExs(alphabet);
            er = null; // Allow for GC.
            log.info(String.format("Read %d test examples", data.size()));

            // Decode the test data.
            log.info("Running the decoder on test data.");
            MbrDecoder decoder = getDecoder();
            decoder.decode(model, data);
            
            AccuracyEvaluator accEval = new AccuracyEvaluator();
            double accuracy = accEval.evaluate(data.getGoldConfigs(), decoder.getMbrVarConfigList());
            log.info("Test accuracy: " + accuracy);

            if (testPredOut != null) {
                ErmaWriter ew = new ErmaWriter();
                ew.writePredictions(testPredOut, decoder.getMbrVarConfigList(), decoder.getVarMargMap());
            }
        }
    }

    private MbrDecoder getDecoder() {
        MbrDecoderPrm decoderPrm = new MbrDecoderPrm();
        decoderPrm.infFactory = getInfFactory();
        decoderPrm.loss = Loss.ACCURACY;
        MbrDecoder decoder = new MbrDecoder(decoderPrm);
        return decoder;
    }

    /* --------- Factory Methods ---------- */
    
    private static CrfTrainerPrm getCrfTrainerPrm() {
        BeliefPropagationPrm bpPrm = getInfFactory();
                
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        prm.maximizer = getMaximizer();
        prm.regularizer = new L2(1.0);
        return prm;
    }

    private static MalletLBFGS getMaximizer() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        MalletLBFGS maximizer = new MalletLBFGS(prm);
        
        // To run with SGD, uncomment these lines.
        //        SGDPrm optPrm = new SGDPrm();
        //        optPrm.iterations = 100;
        //        optPrm.lrAtMidpoint = 0.1;
        //        prm.maximizer = new SGD(optPrm);
        
        return maximizer;
    }

    private static BeliefPropagationPrm getInfFactory() {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = logDomain;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        // TODO: we need to figure out how to compute the log-likelihood AND normalize the marginals.
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;
        return bpPrm;
    }    
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(CrfRunner.class);
        parser.addClass(CrfRunner.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Prng.seed(seed);
        
        CrfRunner pipeline = new CrfRunner();
        try {
            pipeline.run();
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            parser.printUsage();
            System.exit(1);
        }
    }

}
