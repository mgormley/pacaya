package edu.jhu.srl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.SrlGraph;
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
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.data.ErmaReader;
import edu.jhu.gm.data.ErmaWriter;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Files;
import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.dist.Gaussian;

/**
 * Pipeline runner for SRL experiments.
 * @author mgormley
 */
public class SrlRunner {

    public static enum DatasetType { ERMA, CONLL_2009 };

    public static enum InitParams { UNIFORM, RANDOM };
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;

    // Options for data.

    @Opt(hasArg = true, description = "Maximum sentence length for each train/test set.")
    public static int maxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in each train/test set.")
    public static int maxNumSentences = Integer.MAX_VALUE; 
    
    // Options for train data
    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, description = "Type of training data.")
    public static DatasetType trainType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "ERMA feature file.")
    public static File featureFileIn = null;
    @Opt(hasArg = true, description = "Training data predictions output file.")
    public static File trainPredOut = null;

    // Options for test data
    @Opt(hasArg = true, description = "Testing data input file or directory.")
    public static File test = null;
    @Opt(hasArg = true, description = "Type of testing data.")
    public static DatasetType testType = DatasetType.CONLL_2009;
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

    // Options for SRL factor graph structure.
    @Opt(hasArg = true, description = "The structure of the Role variables.")
    public static RoleStructure roleStructure = RoleStructure.ALL_PAIRS;
    @Opt(hasArg = true, description = "Whether Role variables with unknown predicates should be latent.")
    public static boolean makeUnknownPredRolesLatent = true;
    @Opt(hasArg = true, description = "The type of the link variables.")
    public static VarType linkVarType = VarType.LATENT;
    @Opt(hasArg = true, description = "Whether to include a projective dependency tree global factor.")
    public static boolean useProjDepTreeFactor = false;
    @Opt(hasArg = true, description = "Whether to allow a predicate to assign a role to itself. (This should be turned on for English)")
    public static boolean allowPredArgSelfLoops = false;

    // Options for SRL feature extraction.
    @Opt(hasArg = true, description = "Cutoff for OOV words.")
    public static int cutoff = 3;
    @Opt(hasArg = true, description = "For testing only: whether to use only the bias feature.")
    public static boolean biasOnly = false;

    // Options for SRL data munging.
    @Opt(hasArg = true, description = "SRL language.")
    public static String language = "es";
    @Opt(hasArg = true, description = "Whether to use gold POS tags.")
    public static boolean useGoldPos = false;    
    @Opt(hasArg=true, description="Whether to normalize and clean words.")
    public static boolean normalizeWords = false;
    @Opt(hasArg=true, description="Whether to normalize the role names (i.e. lowercase and remove themes).")
    public static boolean normalizeRoleNames = false;

    // Options for training.
    @Opt(hasArg=true, description="Max iterations for L-BFGS training.")
    public static int maxLbfgsIterations = 1000;
    
    public SrlRunner() {
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
            String name = "train";
            // Train a model.
            // TODO: add option for useUnsupportedFeatures.
            FgExamples data = getData(alphabet, trainType, train, name);
            
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
            
            // Decode and evaluate the train data.
            List<VarConfig> predictions = decode(model, data, trainType, trainPredOut, name);        
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
            alphabet.stopGrowth();
            String name = "test";
            FgExamples data = getData(alphabet, testType, test, name);

            // Decode and evaluate the test data.
            List<VarConfig> predictions = decode(model, data, testType, testPredOut, name);
            eval(data, name, predictions);
        }
    }

    private FgExamples getData(Alphabet<Feature> alphabet, DatasetType dataType, File dataFile, String name) throws ParseException, IOException {
        log.info("Reading " + name + " data of type " + dataType + " from " + dataFile);
        FgExamples data;
        if (dataType == DatasetType.CONLL_2009){
            CoNLL09FileReader reader = new CoNLL09FileReader(dataFile);
            List<CoNLL09Sentence> sents = new ArrayList<CoNLL09Sentence>();
            for (CoNLL09Sentence sent : reader) {
                if (sents.size() >= maxNumSentences) {
                    break;
                }
                if (sent.size() <= maxSentenceLength) {
                    sents.add(sent);
                }
            }
            log.info("Num " + name + " sentences: " + sents.size());
            log.info("Building factor graphs and extracting features.");
            SrlFgExampleBuilderPrm prm = getSrlFgExampleBuilderPrm();
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
            data = builder.getData(sents);
        } else if (dataType == DatasetType.ERMA){
            ErmaReader er = new ErmaReader(true);
            data = er.read(featureFileIn, dataFile, alphabet);        
        } else {
            throw new ParseException("Unsupported data type: " + dataType);
        }
        
        log.info(String.format("Num examples in %s: %d", name, data.size()));
        log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num features: %d", data.getAlphabet().size()));
        return data;
    }

    private void eval(FgExamples data, String name, List<VarConfig> predictions) {
        AccuracyEvaluator accEval = new AccuracyEvaluator();
        double accuracy = accEval.evaluate(data.getGoldConfigs(), predictions);
        log.info(String.format("Accuracy on %s: %.6f", name, accuracy));
    }

    private List<VarConfig> decode(FgModel model, FgExamples data, DatasetType dataType, File predOut, String name) throws IOException {
        log.info("Running the decoder on " + name + " data.");
        MbrDecoder decoder = getDecoder();
        decoder.decode(model, data);

        List<VarConfig> predictions = decoder.getMbrVarConfigList();
        if (predOut != null) {
            log.info("Writing predictions for " + name + " data of type " + dataType + " to " + predOut);
            if (dataType == DatasetType.CONLL_2009) {
                @SuppressWarnings("unchecked")
                List<CoNLL09Sentence> sents = (List<CoNLL09Sentence>)data.getSourceSentences();
                for (int i=0; i<sents.size(); i++) {
                    VarConfig vc = predictions.get(i);
                    CoNLL09Sentence sent = sents.get(i);
                    
                    SrlGraph srlGraph = SrlDecoder.getSrlGraphFromVarConfig(vc, sent);
                    
                    sent.setPredApredFromSrlGraph(srlGraph, false);
                }
                CoNLL09Writer cw = new CoNLL09Writer(predOut);
                for (CoNLL09Sentence sent : sents) {
                    cw.write(sent);
                }
                cw.close();
            } else {
                ErmaWriter ew = new ErmaWriter();
                ew.writePredictions(predOut, predictions, decoder.getVarMargMap());
            }
        }
        return predictions;
    }

    

    /* --------- Factory Methods ---------- */

    private static SrlFgExampleBuilderPrm getSrlFgExampleBuilderPrm() {
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        // Factor graph structure.
        prm.fgPrm.linkVarType = linkVarType;
        prm.fgPrm.makeUnknownPredRolesLatent = makeUnknownPredRolesLatent;
        prm.fgPrm.roleStructure = roleStructure;
        prm.fgPrm.useProjDepTreeFactor = useProjDepTreeFactor;
        prm.fgPrm.allowPredArgSelfLoops = allowPredArgSelfLoops;
        // Feature extraction.
        prm.fePrm.cutoff = cutoff;
        prm.fePrm.biasOnly = biasOnly;
        prm.fePrm.language = language;
        prm.fePrm.useGoldPos = useGoldPos;
        prm.fePrm.normalizeWords = normalizeWords;
        prm.fePrm.normalizeRoleNames = normalizeRoleNames;
        return prm;
    }
    
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
        prm.maxIterations = maxLbfgsIterations;
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
        ArgParser parser = new ArgParser(SrlRunner.class);
        parser.addClass(SrlRunner.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Prng.seed(seed);
        
        SrlRunner pipeline = new SrlRunner();
        try {
            pipeline.run();
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            parser.printUsage();
            System.exit(1);
        }
    }

}
