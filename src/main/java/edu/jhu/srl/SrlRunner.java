package edu.jhu.srl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL08Sentence;
import edu.jhu.data.conll.CoNLL08Writer;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.simple.CorpusHandler;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateReader;
import edu.jhu.featurize.TemplateSets;
import edu.jhu.featurize.TemplateWriter;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.gm.decode.MbrDecoder.Loss;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.eval.AccuracyEvaluator;
import edu.jhu.gm.eval.AccuracyEvaluator.VarConfigPair;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
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
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.InformationGainFeatureTemplateSelectorPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.SrlFeatTemplates;
import edu.jhu.srl.SrlDecoder.SrlDecoderPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.files.Files;

/**
 * Pipeline runner for SRL experiments.
 * @author mgormley
 * @author mmitchell
 */
public class SrlRunner {

    public static enum DatasetType { ERMA, CONLL_2009, CONLL_2008 };

    public static enum InitParams { UNIFORM, RANDOM };
    
    public static enum Optimizer { LBFGS, SGD, ADAGRAD, ADADELTA };
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    
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

    // Options for SRL feature selection.
    @Opt(hasArg = true, description = "Whether to do feature selection.")
    public static boolean featureSelection = true;
    @Opt(hasArg = true, description = "The number of feature bigrams to form.")
    public static int numFeatsToSelect = 32;
    @Opt(hasArg = true, description = "The max number of sentences to use for feature selection")
    public static int numSentsForFeatSelect = 1000;    
    
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
    @Opt(hasArg = true, description = "Whether to add the Bjorkelund features.")
    public static boolean useBjorkelundFeats = true;
    @Opt(hasArg = true, description = "Whether to add dependency path features.")
    public static boolean useLexicalDepPathFeats = false;
    @Opt(hasArg = true, description = "Whether to include pairs of features.")
    public static boolean useTemplates = false;
    @Opt(hasArg = true, description = "Sense feature templates.")
    public static String senseFeatTpls = TemplateSets.bjorkelundSenseFeatsResource;
    @Opt(hasArg = true, description = "Arg feature templates.")
    public static String argFeatTpls = TemplateSets.bjorkelundArgFeatsResource;
    @Opt(hasArg = true, description = "Sense feature template output file.")
    public static File senseFeatTplsOut = null;
    @Opt(hasArg = true, description = "Arg feature template output file.")
    public static File argFeatTplsOut = null;
    
    // Options for data munging.
    @Deprecated
    @Opt(hasArg=true, description="Whether to normalize and clean words.")
    public static boolean normalizeWords = false;

    // Options for caching.
    @Opt(hasArg = true, description = "The type of cache/store to use for training/testing instances.")
    public static CacheType cacheType = CacheType.CACHE;
    @Opt(hasArg = true, description = "When caching, the maximum number of examples to keep cached in memory or -1 for SoftReference caching.")
    public static int maxEntriesInMemory = 100;
    @Opt(hasArg = true, description = "Whether to gzip an object before caching it.")
    public static boolean gzipCache = false;    
    
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
    @Opt(hasArg=true, description="The constant addend for AdaGrad.")
    public static double adaGradConstantAddend = 1e-9;
    @Opt(hasArg=true, description="The decay rate for AdaDelta.")
    public static double adaDeltaDecayRate = 0.95;
    @Opt(hasArg=true, description="The constant addend for AdaDelta.")
    public static double adaDeltaConstantAddend = Math.pow(Math.E, -6.);
    @Opt(hasArg=true, description="Stop training by this date/time.")
    public static Date stopTrainingBy = null;

    public SrlRunner() {
    }

    public void run() throws ParseException, IOException {  
        if (logDomain) {
            FastMath.useLogAddTable = true;
        }
        if (stopTrainingBy != null && new Date().after(stopTrainingBy)) {
            log.warn("Training will never begin since stopTrainingBy has already happened: " + stopTrainingBy);
            log.warn("Ignoring stopTrainingBy by setting it to null.");
            stopTrainingBy = null;
        }
        
        // Initialize the data reader/writer.
        CorpusHandler corpus = new CorpusHandler();
        
        // Get a model.
        SrlFgModel model = null;
        ObsFeatureConjoiner ofc;
        FactorTemplateList fts;
        CorpusStatistics cs;
        SrlFeatureExtractorPrm srlFePrm;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (SrlFgModel) Files.deserialize(modelIn);
            ofc = model.getOfc();
            fts = ofc.getTemplates();
            cs = model.getCs();
            srlFePrm = model.getSrlFePrm();            
            // TODO: use atList here.
        } else {
            srlFePrm = getSrlFeatureExtractorPrm();
            removeAts(srlFePrm);
            cs = new CorpusStatistics(getCorpusStatisticsPrm());
            featureSelection(corpus.getTrainGold(), cs, srlFePrm);
            fts = new FactorTemplateList();
            ofc = new ObsFeatureConjoiner(getObsFeatureConjoinerPrm(), fts);
        }

        if (corpus.hasTrain()) {
            String name = "train";
            // Train a model.
            SimpleAnnoSentenceCollection goldSents = corpus.getTrainGold();
            FgExampleList data = getData(ofc, cs, name, goldSents, srlFePrm);
            
            if (model == null) {
                model = new SrlFgModel(cs, ofc, srlFePrm);
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
            log.info(String.format("Num model params: %d", model.getNumParams()));

            log.info("Training model.");
            CrfTrainerPrm prm = getCrfTrainerPrm();
            CrfTrainer trainer = new CrfTrainer(prm);
            trainer.train(model, data);
            trainer = null; // Allow for GC.
            
            // Decode and evaluate the train data.
            SimpleAnnoSentenceCollection predSents = decode(model, data, goldSents, name);
            corpus.writeTrainPreds(predSents);
        }
          
        if (modelOut != null) {
            // Write the model to a file.
            log.info("Serializing model to file: " + modelOut);
            Files.serialize(model, modelOut);
        }
        if (printModel != null) {
            // Print the model to a file.
            log.info("Printing human readable model to file: " + printModel);            
            OutputStream os = new FileOutputStream(printModel);
            if (printModel.getName().endsWith(".gz")) {
                os = new GZIPOutputStream(os);
            }
            Writer writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            model.printModel(writer);
            writer.close();
        }

        if (corpus.hasDev()) {
            // Test the model on dev data.
            fts.stopGrowth();
            String name = "dev";
            SimpleAnnoSentenceCollection goldSents = corpus.getDevGold();
            FgExampleList data = getData(ofc, cs, name, goldSents, srlFePrm);
            // Decode and evaluate the dev data.
            SimpleAnnoSentenceCollection predSents = decode(model, data, goldSents, name);
            corpus.writeDevPreds(predSents);
        }
        
        if (corpus.hasTest()) {
            // Test the model on test data.
            fts.stopGrowth();
            String name = "test";
            SimpleAnnoSentenceCollection goldSents = corpus.getTestGold();
            FgExampleList data = getData(ofc, cs, name, goldSents, srlFePrm);
            // Decode and evaluate the test data.
            SimpleAnnoSentenceCollection predSents = decode(model, data, goldSents, name);
            corpus.writeTestPreds(predSents);
        }
    }

    /**
     * Do feature selection and update srlFePrm with the chosen feature templates.
     */
    private void featureSelection(SimpleAnnoSentenceCollection sents, CorpusStatistics cs, SrlFeatureExtractorPrm srlFePrm) throws IOException,
            ParseException {
        if (useTemplates && featureSelection) {
            CorpusStatisticsPrm csPrm = getCorpusStatisticsPrm();
            
            InformationGainFeatureTemplateSelectorPrm prm = new InformationGainFeatureTemplateSelectorPrm();
            prm.featureHashMod = featureHashMod;
            prm.numThreads = threads;
            prm.numToSelect = numFeatsToSelect;
            prm.maxNumSentences = numSentsForFeatSelect;
            prm.selectSense = predictSense;
            SrlFeatTemplates sft = new SrlFeatTemplates(srlFePrm.fePrm.soloTemplates, srlFePrm.fePrm.pairTemplates, null);
            InformationGainFeatureTemplateSelector ig = new InformationGainFeatureTemplateSelector(prm);
            sft = ig.getFeatTemplatesForSrl(sents, csPrm, sft);
            ig.shutdown();
            srlFePrm.fePrm.soloTemplates = sft.srlSense;
            srlFePrm.fePrm.pairTemplates = sft.srlArg;
            removeAts(srlFePrm); // TODO: This probably isn't necessary, but just in case.
        }
        if (useTemplates) {
            log.info("Num sense feature templates: " + srlFePrm.fePrm.soloTemplates.size());
            log.info("Num arg feature templates: " + srlFePrm.fePrm.pairTemplates.size());
            if (senseFeatTplsOut != null) {
                TemplateWriter.write(senseFeatTplsOut, srlFePrm.fePrm.soloTemplates);
            }
            if (argFeatTplsOut != null) {
                TemplateWriter.write(argFeatTplsOut, srlFePrm.fePrm.pairTemplates);
            }
        }
    }

    private void removeAts(SrlFeatureExtractorPrm srlFePrm) {
        for (AT at : CorpusHandler.getAts(CorpusHandler.removeAts)) {
            srlFePrm.fePrm.soloTemplates = TemplateLanguage.filterOutRequiring(srlFePrm.fePrm.soloTemplates, at);
            srlFePrm.fePrm.pairTemplates   = TemplateLanguage.filterOutRequiring(srlFePrm.fePrm.pairTemplates, at);
        }
    }

    private FgExampleList getData(ObsFeatureConjoiner ofc, CorpusStatistics cs, String name,
            SimpleAnnoSentenceCollection sents, SrlFeatureExtractorPrm srlFePrm) {
        if (!cs.isInitialized()) {
            log.info("Initializing corpus statistics.");
            cs.init(sents);
        }
        FactorTemplateList fts = ofc.getTemplates();
        
        if (useTemplates) {
            SimpleAnnoSentence sent = sents.get(0);
            TemplateLanguage.assertRequiredAnnotationTypes(sent, srlFePrm.fePrm.soloTemplates);
            TemplateLanguage.assertRequiredAnnotationTypes(sent, srlFePrm.fePrm.pairTemplates);
        }
        
        log.info("Building factor graphs and extracting features.");
        SrlFgExampleBuilderPrm prm = getSrlFgExampleBuilderPrm(srlFePrm);        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, ofc, cs);
        FgExampleList data = builder.getData(sents);
        
        // Special case: we somehow need to be able to create test examples
        // where we've never seen the predicate.
        if (prm.fgPrm.predictSense && fts.isGrowing()) {
            // TODO: This should have a bias feature.
            Var v = new Var(VarType.PREDICTED, 1, CorpusStatistics.UNKNOWN_SENSE, CorpusStatistics.SENSES_FOR_UNK_PRED);
            fts.add(new FactorTemplate(new VarSet(v), new Alphabet<Feature>(), SrlFactorGraph.TEMPLATE_KEY_FOR_UNKNOWN_SENSE));
        }
        
        if (!ofc.isInitialized()) {
            log.info("Initializing the observation function conjoiner.");
            ofc.init(data);
        }
        
        log.info(String.format("Num examples in %s: %d", name, data.size()));
        // TODO: log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        // TODO: log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num factor/clique templates: %d", fts.size()));
        log.info(String.format("Num observation function features: %d", fts.getNumObsFeats()));
        return data;
    }
    
    private void eval(String name, VarConfigPair pair) {
        AccuracyEvaluator accEval = new AccuracyEvaluator();
        double accuracy = accEval.evaluate(pair.gold, pair.pred);
        log.info(String.format("Accuracy on %s: %.6f", name, accuracy));
    }
    
    // TODO: This should take the input sentences and add predictions.
    private SimpleAnnoSentenceCollection decode(FgModel model, FgExampleList data, SimpleAnnoSentenceCollection goldSents, String name) throws IOException, ParseException {
        log.info("Running the decoder on " + name + " data.");

        // Predicted sentences
        SimpleAnnoSentenceCollection predSents = new SimpleAnnoSentenceCollection();
        List<VarConfig> predVcs = new ArrayList<VarConfig>();
        List<VarConfig> goldVcs = new ArrayList<VarConfig>();
        
        for (int i=0; i< goldSents.size(); i++) {
            FgExample ex = data.get(i);
            SimpleAnnoSentence goldSent = goldSents.get(i);
            SimpleAnnoSentence predSent = new SimpleAnnoSentence(goldSent);
            SrlDecoder decoder = getDecoder();
            decoder.decode(model, ex);
            
            // Get the MBR variable assignment.
            VarConfig predVc = decoder.getMbrVarConfig();
            predVcs.add(predVc);
            
            // Update SRL graph on the sentence. 
            SrlGraph srlGraph = decoder.getSrlGraph();
            predSent.setSrlGraph(srlGraph);
            // Update the dependency tree on the sentence.
            int[] parents = decoder.getParents();
            if (parents != null) {
                predSent.setParents(parents);
            }            
            predSents.add(predSent);
            
            // Get the gold variable assignment.
            goldVcs.add(ex.getGoldConfig());
        }
           
        // Simple accuracy check of factor graph variables.
        eval(name, new VarConfigPair(goldVcs, predVcs));
        
        return predSents;
    }

    

    /* --------- Factory Methods ---------- */
    
    private static SrlFgExampleBuilderPrm getSrlFgExampleBuilderPrm(SrlFeatureExtractorPrm srlFePrm) {
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
        prm.srlFePrm = srlFePrm;
        
        // Example construction and storage.
        prm.exPrm.cacheType = cacheType;
        prm.exPrm.gzipped = gzipCache;
        prm.exPrm.maxEntriesInMemory = maxEntriesInMemory;
        
        // SRL Feature Extraction.
        prm.srlFePrm.featureHashMod = featureHashMod;
        return prm;
    }
    
    private static ObsFeatureConjoinerPrm getObsFeatureConjoinerPrm() {
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.featCountCutoff = featCountCutoff;
        prm.includeUnsupportedFeatures = includeUnsupportedFeatures;
        return prm;
    }
    
    private static SrlFeatureExtractorPrm getSrlFeatureExtractorPrm() {
        SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();
        srlFePrm.fePrm.biasOnly = biasOnly;
        srlFePrm.fePrm.useSimpleFeats = useSimpleFeats;
        srlFePrm.fePrm.useNaradFeats = useNaradFeats;
        srlFePrm.fePrm.useZhaoFeats = useZhaoFeats;
        srlFePrm.fePrm.useBjorkelundFeats = useBjorkelundFeats;
        srlFePrm.fePrm.useLexicalDepPathFeats = useLexicalDepPathFeats;
        srlFePrm.fePrm.useTemplates = useTemplates;
        
        srlFePrm.fePrm.soloTemplates = getFeatTpls(senseFeatTpls);
        srlFePrm.fePrm.pairTemplates = getFeatTpls(argFeatTpls);
        
        return srlFePrm;
    }

    /**
     * Gets feature templates from multiple files or resources.
     * @param featTpls A colon separated list of paths to feature template files or resources.
     * @return The feature templates from all the paths.
     */
    private static List<FeatTemplate> getFeatTpls(String featTpls) {
        Collection<FeatTemplate> tpls = new LinkedHashSet<FeatTemplate>();

        TemplateReader tr = new TemplateReader();
        for (String path : featTpls.split(":")) {
            if (path.equals("coarse1") || path.equals("coarse2")) {
                List<FeatTemplate> coarseUnigramSet;
                if (path.equals("coarse1")) { 
                    coarseUnigramSet = TemplateSets.getCoarseUnigramSet1();
                } else if (path.equals("coarse2")) {
                    coarseUnigramSet = TemplateSets.getCoarseUnigramSet2();
                } else {
                    throw new IllegalStateException();
                }
                if (CorpusHandler.brownClusters == null) {
                    // Filter out the Brown cluster features.
                    log.warn("Filtering out Brown cluster features from coarse set.");
                    coarseUnigramSet = TemplateLanguage.filterOutRequiring(coarseUnigramSet, AT.BROWN);
                }
                tpls.addAll(coarseUnigramSet);
            } else {
                try {
                    tr.readFromFile(path);
                } catch (IOException e) {
                    try {
                        tr.readFromResource(path);
                    } catch (IOException e1) {
                        throw new IllegalStateException("Unable to read templates as file or resource: " + path, e1);
                    }
                }
            }
        }
        tpls.addAll(tr.getTemplates());
        
        return new ArrayList<FeatTemplate>(tpls);
    }

    private static CorpusStatisticsPrm getCorpusStatisticsPrm() {
        CorpusStatisticsPrm prm = new CorpusStatisticsPrm();
        prm.cutoff = cutoff;
        prm.language = CorpusHandler.language;
        prm.useGoldSyntax = CorpusHandler.useGoldSyntax;
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
        prm.numThreads = threads;
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
        prm.stopBy = stopTrainingBy;
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

    private SrlDecoder getDecoder() {
        MbrDecoderPrm mbrPrm = new MbrDecoderPrm();
        mbrPrm.infFactory = getInfFactory();
        mbrPrm.loss = Loss.ACCURACY;
        SrlDecoderPrm prm = new SrlDecoderPrm();
        prm.mbrPrm = mbrPrm;
        return new SrlDecoder(prm);
    }
    
    public static void main(String[] args) {
        try {
            ArgParser parser = new ArgParser(SrlRunner.class);
            parser.addClass(SrlRunner.class);
            parser.addClass(CorpusHandler.class);
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
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

}
