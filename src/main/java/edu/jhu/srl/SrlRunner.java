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
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.gm.AccuracyEvaluator;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfTrainer;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplate;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.MbrDecoder;
import edu.jhu.gm.MbrDecoder.Loss;
import edu.jhu.gm.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.gm.data.ErmaWriter;
import edu.jhu.optimize.AdaDelta;
import edu.jhu.optimize.AdaDelta.AdaDeltaPrm;
import edu.jhu.optimize.AdaGrad;
import edu.jhu.optimize.AdaGrad.AdaGradPrm;
import edu.jhu.optimize.L2;
import edu.jhu.optimize.MalletLBFGS;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.optimize.Maximizer;
import edu.jhu.optimize.SGD;
import edu.jhu.optimize.SGD.SGDPrm;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Files;
import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Pipeline runner for SRL experiments.
 * @author mgormley
 */
public class SrlRunner {

    public static enum DatasetType { ERMA, CONLL_2009 };

    public static enum InitParams { UNIFORM, RANDOM };
    
    public static enum Optimizer { LBFGS, SGD, ADAGRAD, ADADELTA };
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;

    // Options for train data
    @Opt(hasArg = true, description = "Training data input file or directory.")
    public static File train = null;
    @Opt(hasArg = true, description = "Type of training data.")
    public static DatasetType trainType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "ERMA feature file.")
    public static File featureFileIn = null;
    @Opt(hasArg = true, description = "Training data predictions output file.")
    public static File trainPredOut = null;
    @Opt(hasArg = true, description = "Training data gold output file.")
    public static File trainGoldOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for train.")
    public static int trainMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in train.")
    public static int trainMaxNumSentences = Integer.MAX_VALUE; 
    
    // Options for test data
    @Opt(hasArg = true, description = "Testing data input file or directory.")
    public static File test = null;
    @Opt(hasArg = true, description = "Type of testing data.")
    public static DatasetType testType = DatasetType.CONLL_2009;
    @Opt(hasArg = true, description = "Testing data predictions output file.")
    public static File testPredOut = null;
    @Opt(hasArg = true, description = "Testing data gold output file.")
    public static File testGoldOut = null;
    @Opt(hasArg = true, description = "Maximum sentence length for test.")
    public static int testMaxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences to include in test.")
    public static int testMaxNumSentences = Integer.MAX_VALUE; 

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
    public static RoleStructure roleStructure = RoleStructure.PREDS_GIVEN;
    @Opt(hasArg = true, description = "Whether Role variables with unknown predicates should be latent.")
    public static boolean makeUnknownPredRolesLatent = true;
    @Opt(hasArg = true, description = "The type of the link variables.")
    public static VarType linkVarType = VarType.LATENT;
    @Opt(hasArg = true, description = "Whether to include a projective dependency tree global factor.")
    public static boolean useProjDepTreeFactor = false;
    @Opt(hasArg = true, description = "Whether to allow a predicate to assign a role to itself. (This should be turned on for English)")
    public static boolean allowPredArgSelfLoops = false;
    @Opt(hasArg = true, description = "Whether to include unary factors in the model. (Ignored if there are no Link variables.)")
    public static boolean unaryFactors = false;
    @Opt(hasArg = true, description = "Whether to always include Link variables. For testing only.")
    public static boolean alwaysIncludeLinkVars = false;
    @Opt(hasArg = true, description = "Whether to predict predicate sense.")
    public static boolean predictSense = false;

    // Options for SRL feature extraction.
    @Opt(hasArg = true, description = "Cutoff for OOV words.")
    public static int cutoff = 3;
    @Opt(hasArg = true, description = "For preprocessing: Minimum feature count for caching.")
    public static int featCountCutoff = 4;
    @Opt(hasArg = true, description = "For testing only: whether to use only the bias feature.")
    public static boolean biasOnly = false;
    @Opt(hasArg = true, description = "The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled.")
    public static int featureHashMod = 524288; // 2^19
    @Opt(hasArg = true, description = "Whether to include unsupported features.")
    public static boolean includeUnsupportedFeatures = false;
    @Opt(hasArg = true, description = "Whether to add the Simple features.")
    public static boolean useSimpleFeats = true;
    @Opt(hasArg = true, description = "Whether to add the Naradowsky features.")
    public static boolean useNaradFeats = true;
    @Opt(hasArg = true, description = "Whether to add the Zhao features.")
    public static boolean useZhaoFeats = true;
    @Opt(hasArg = true, description = "Whether to add dependency path features.")
    public static boolean useDepPathFeats = true;

    // Options for SRL data munging.
    @Opt(hasArg = true, description = "SRL language.")
    public static String language = "es";
    
    // Options for data munging.
    @Opt(hasArg = true, description = "Whether to use gold POS tags.")
    public static boolean useGoldSyntax = false;    
    @Opt(hasArg=true, description="Whether to normalize and clean words.")
    public static boolean normalizeWords = false;
    @Opt(hasArg=true, description="Whether to normalize the role names (i.e. lowercase and remove themes).")
    public static boolean normalizeRoleNames = false;
    @Opt(hasArg = true, description = "Whether to remove the deprel and pdeprel columns from CoNLL-2009 data.")
    public static boolean removeDeprel = false;

    // Options for training.
    @Opt(hasArg=true, description="The optimization method to use for training.")
    public static Optimizer optimizer = Optimizer.LBFGS;
    @Opt(hasArg=true, description="The variance for the L2 regularizer.")
    public static double l2variance = 1.0;
    @Opt(hasArg=true, description="Max iterations for L-BFGS training.")
    public static int maxLbfgsIterations = 1000;
    @Opt(hasArg=true, description="Number of effective passes over the dataset for SGD.")
    public static double sgdNumPasses = 30;
    @Opt(hasArg=true, description="The batch size to use at each step of SGD.")
    public static int sgdBatchSize = 15;
    @Opt(hasArg=true, description="The initial learning rate for SGD.")
    public static double sgdInitialLr = 0.1;
    @Opt(hasArg=true, description="Whether to sample with replacement for SGD.")
    public static boolean sgdWithRepl = false;    
    @Opt(hasArg=true, description="The AdaGrad parameter for scaling the learning rate.")
    public static double adaGradEta = 0.1;
    @Opt(hasArg=true, description="The decay rate for AdaDelta.")
    public static double adaDeltaDecayRate = 0.95;
    @Opt(hasArg=true, description="The constant addend for AdaDelta.")
    public static double adaDeltaConstantAddend = Math.pow(Math.E, -6.);

    public SrlRunner() {
    }

    public void run() throws ParseException, IOException {  
        if (logDomain) {
            Utilities.useLogAddTable = true;
        }
        
        // Get a model.
        SrlFgModel model = null;
        FeatureTemplateList fts;
        CorpusStatistics cs;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (SrlFgModel) Files.deserialize(modelIn);
            fts = model.getTemplates();
            cs = model.getCs();
        } else {
            fts = new FeatureTemplateList();
            cs = new CorpusStatistics(getCorpusStatisticsPrm());
        }
        
        if (trainType != null && train != null) {
            String name = "train";
            // Train a model.
            // TODO: add option for useUnsupportedFeatures.
            FgExamples data = getData(fts, cs, trainType, train, trainGoldOut, trainMaxNumSentences,
                    trainMaxSentenceLength, name);
            
            if (model == null) {
                model = new SrlFgModel(data, includeUnsupportedFeatures, cs);
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
            log.info(String.format("Num features: %d", model.getNumParams()));

            log.info("Training model.");
            CrfTrainerPrm prm = getCrfTrainerPrm();
            CrfTrainer trainer = new CrfTrainer(prm);
            trainer.train(model, data);
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
            fts.stopGrowth();
            String name = "test";
            FgExamples data = getData(fts, cs, testType, test, testGoldOut, testMaxNumSentences,
                    testMaxSentenceLength, name);
            // Decode and evaluate the test data.
            List<VarConfig> predictions = decode(model, data, testType, testPredOut, name);
            eval(data, name, predictions);
        }
    }

    private FgExamples getData(FeatureTemplateList fts, CorpusStatistics cs, DatasetType dataType, File dataFile, File goldFile,
            int maxNumSentences, int maxSentenceLength, String name) throws ParseException, IOException {
        log.info("Reading " + name + " data of type " + dataType + " from " + dataFile);
        FgExamples data;
        if (dataType == DatasetType.CONLL_2009){
            CoNLL09FileReader reader = new CoNLL09FileReader(dataFile);
            List<CoNLL09Sentence> sents = new ArrayList<CoNLL09Sentence>();
            int numTokens = 0;
            for (CoNLL09Sentence sent : reader) {
                if (sents.size() >= maxNumSentences) {
                    break;
                }
                if (sent.size() <= maxSentenceLength) {
                    sents.add(sent);
                    numTokens += sent.size();
                }
            }
            reader.close();
            
            if (normalizeRoleNames) {
                log.info("Normalizing role names");
                CorpusStatistics.normalizeRoleNames(sents);
            }
            
            log.info("Num " + name + " sentences: " + sents.size());   
            log.info("Num " + name + " tokens: " + numTokens);

            if (goldFile != null) {
                log.info("Writing gold data to file: " + goldFile);
                CoNLL09Writer cw = new CoNLL09Writer(goldFile);
                for (CoNLL09Sentence sent : sents) {
                    cw.write(sent);
                }
                cw.close();
            }
            
            if (useProjDepTreeFactor) {
                log.info("Removing all dependency trees from the CoNLL data");
                for (CoNLL09Sentence sent : sents) {
                    for (CoNLL09Token tok : sent) {
                        tok.setPhead(0);
                        tok.setHead(0);
                        tok.setDeprel("_");
                        tok.setPdeprel("_");
                    }
                }
            } else if (removeDeprel) {
                log.info("Removing syntactic dependency labels from the CoNLL data");                
                for (CoNLL09Sentence sent : sents) {
                    for (CoNLL09Token tok : sent) {
                        tok.setDeprel("_");
                        tok.setPdeprel("_");
                    }
                }
            }
            
            if (!cs.isInitialized()) {
                log.info("Initializing corpus statistics.");
                cs.init(sents);
            }
            
            log.info("Building factor graphs and extracting features.");
            SrlFgExampleBuilderPrm prm = getSrlFgExampleBuilderPrm();
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
            data = builder.getData(sents);     

            // Special case: we somehow need to be able to create test examples
            // where we've never seen the predicate.
            if (prm.fgPrm.predictSense) {
                Var v = new Var(VarType.PREDICTED, 1, CorpusStatistics.UNKNOWN_SENSE, CorpusStatistics.SENSES_FOR_UNK_PRED);
                fts.add(new FeatureTemplate(new VarSet(v), new Alphabet<Feature>(), SrlFactorGraph.TEMPLATE_KEY_FOR_UNKNOWN_SENSE));
            }
        } else {
            throw new ParseException("Unsupported data type: " + dataType);
        }
        
        log.info(String.format("Num examples in %s: %d", name, data.size()));
        log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num feature templates: %d", data.getTemplates().size()));
        log.info(String.format("Num observation function features: %d", data.getTemplates().getNumObsFeats()));
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
        prm.fgPrm.unaryFactors = unaryFactors;
        prm.fgPrm.alwaysIncludeLinkVars = alwaysIncludeLinkVars;
        prm.fgPrm.predictSense = predictSense;
        // Feature extraction.
        prm.fePrm.biasOnly = biasOnly;
        prm.fePrm.useSimpleFeats = useSimpleFeats;
        prm.fePrm.useNaradFeats = useNaradFeats;
        prm.fePrm.useZhaoFeats = useZhaoFeats;
        prm.fePrm.useDepPathFeats = useDepPathFeats;
        prm.featCountCutoff = featCountCutoff;
        // SRL Feature Extraction.
        prm.srlFePrm.featureHashMod = featureHashMod;
        return prm;
    }

    private static CorpusStatisticsPrm getCorpusStatisticsPrm() {
        CorpusStatisticsPrm prm = new CorpusStatisticsPrm();
        prm.cutoff = cutoff;
        prm.language = language;
        prm.useGoldSyntax = useGoldSyntax;
        prm.normalizeWords = normalizeWords;
        return prm;
    }
    
    private static CrfTrainerPrm getCrfTrainerPrm() {
        BeliefPropagationPrm bpPrm = getInfFactory();
                
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        if (optimizer == Optimizer.LBFGS) {
            prm.maximizer = getLbfgs();
            prm.batchMaximizer = null;
        } else if (optimizer == Optimizer.SGD){
            prm.maximizer = null;
            prm.batchMaximizer = new SGD(getSgdPrm());
        } else if (optimizer == Optimizer.ADAGRAD){
            prm.maximizer = null;
            AdaGradPrm adaGradPrm = new AdaGradPrm();
            adaGradPrm.sgdPrm = getSgdPrm();
            adaGradPrm.eta = adaGradEta;
            prm.batchMaximizer = new AdaGrad(adaGradPrm);
        } else if (optimizer == Optimizer.ADADELTA){
            prm.maximizer = null;
            AdaDeltaPrm adaDeltaPrm = new AdaDeltaPrm();
            adaDeltaPrm.sgdPrm = getSgdPrm();
            adaDeltaPrm.decayRate = adaDeltaDecayRate;
            adaDeltaPrm.constantAddend = adaDeltaConstantAddend;
            prm.batchMaximizer = new AdaDelta(adaDeltaPrm);
        } else {
            throw new RuntimeException("Optimizer not supported: " + optimizer);
        }
        prm.regularizer = new L2(l2variance);
        return prm;
    }

    private static Maximizer getLbfgs() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        prm.maxIterations = maxLbfgsIterations;
        return new MalletLBFGS(prm);
    }
    
    private static SGDPrm getSgdPrm() {
        SGDPrm prm = new SGDPrm();
        prm.numPasses = sgdNumPasses;
        prm.batchSize = sgdBatchSize;
        prm.initialLr = sgdInitialLr;
        prm.withReplacement = sgdWithRepl;
        prm.lambda = 1.0 / l2variance;
        return prm;
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
