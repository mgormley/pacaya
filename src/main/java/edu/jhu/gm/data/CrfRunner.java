package edu.jhu.gm.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.gm.AccuracyEvaluator;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfTrainer;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.DenseFactor;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.MbrDecoder;
import edu.jhu.gm.MbrDecoder.Loss;
import edu.jhu.gm.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.util.Files;
import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Runner for the CRF library. This is meant to be used for arbitrary graphical
 * model input files (e.g. ERMA format) and shouldn't be specialized to any
 * particular model.
 * 
 * @author mgormley
 */
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
    public static File modelIn = null;
    @Opt(hasArg = true, description = "File to which we should serialize the model.")
    public static File modelOut = null;
    @Opt(hasArg = true, description = "File to which we should print a human readable version of the model.")
    public static File printModel = null;

    // Options for initialization.
    @Opt(hasArg = true, description = "How to initialize the parameters of the model.")
    public static InitParams initParams = InitParams.UNIFORM;
    
    // Options for inference.
    @Opt(hasArg = true, description = "Whether to run inference in the log-domain.")
    public static boolean logDomain = true;

    // Options for features.
    @Opt(hasArg = true, description = "Whether to include unsupported features.")
    public static boolean includeUnsupportedFeatures = true;
    
    public CrfRunner() {
    }

    public void run() throws ParseException, IOException {  
        if (logDomain) {
            Utilities.useLogAddTable = true;
        }
        
        // Get a model.
        FgModel model = null;
        FeatureTemplateList templates;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (FgModel) Files.deserialize(modelIn);
            templates = model.getTemplates();
        } else {
            templates = new FeatureTemplateList();
        }
        
        if (trainType != null && train != null) {
            String name = "train";
            // Train a model.
            FgExamples data = getData(templates, trainType, train, name);
            
            if (model == null) {
                model = new FgModel(data, includeUnsupportedFeatures);
                if (initParams == InitParams.RANDOM) {
                    model.setRandomStandardNormal();
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
            trainer.train(model, data);
            trainer = null; // Allow for GC.
            
            // Decode and evaluate the train data.
            List<VarConfig> predictions = decode(model, data, trainPredOut, name);        
            eval(data, name, predictions);
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
            // Test the model on test data.
            templates.stopGrowth();
            String name = "test";
            FgExamples data = getData(templates, testType, test, name);

            // Decode and evaluate the test data.
            List<VarConfig> predictions = decode(model, data, testPredOut, name);
            eval(data, name, predictions);
        }
    }

    private FgExamples getData(FeatureTemplateList templates, DatasetType dataType, File dataFile, String name) throws ParseException, IOException {
        FgExamples data;
        if (dataType == DatasetType.ERMA){
            ErmaReader er = new ErmaReader();
            data = er.read(featureFileIn, dataFile, templates);        
        } else {
            throw new ParseException("Unsupported data type: " + dataType);
        }
        
        log.info(String.format("Num examples in %s: %d", name, data.size()));
        log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num observation features: %d", templates.getNumObsFeats()));
        return data;
    }

    private void eval(FgExamples data, String name, List<VarConfig> predictions) {
        AccuracyEvaluator accEval = new AccuracyEvaluator();
        double accuracy = accEval.evaluate(data.getGoldConfigs(), predictions);
        log.info(String.format("Accuracy on %s: %.6f", name, accuracy));
    }

    private List<VarConfig> decode(FgModel model, FgExamples data, File predOut, String name) throws IOException {
        log.info("Running the decoder on " + name + " data.");
        List<VarConfig> predictions = new ArrayList<VarConfig>();
        HashMap<Var,Double> varMargMap = new HashMap<Var,Double>();

        for (int i=0; i<data.size(); i++) {
            MbrDecoder decoder = getDecoder();
            decoder.decode(model, data.get(i));
            predictions.add(decoder.getMbrVarConfig());
            varMargMap.putAll(decoder.getVarMargMap());
        }
        if (predOut != null) {
            ErmaWriter ew = new ErmaWriter();
            ew.writePredictions(predOut, predictions, varMargMap);
        }
        return predictions;
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

    private MbrDecoder getDecoder() {
        MbrDecoderPrm decoderPrm = new MbrDecoderPrm();
        decoderPrm.infFactory = getInfFactory();
        decoderPrm.loss = Loss.ACCURACY;
        MbrDecoder decoder = new MbrDecoder(decoderPrm);
        return decoder;
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
