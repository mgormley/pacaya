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
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.simple.CorpusHandler;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.eval.DepParseEvaluator;
import edu.jhu.featurize.TemplateLanguage;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateReader;
import edu.jhu.featurize.TemplateSets;
import edu.jhu.featurize.TemplateWriter;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.gm.data.UFgExample;
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
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.hlt.optimize.AdaDelta;
import edu.jhu.hlt.optimize.AdaDelta.AdaDeltaPrm;
import edu.jhu.hlt.optimize.AdaGrad;
import edu.jhu.hlt.optimize.AdaGrad.AdaGradPrm;
import edu.jhu.hlt.optimize.BottouSchedule;
import edu.jhu.hlt.optimize.BottouSchedule.BottouSchedulePrm;
import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.SGD;
import edu.jhu.hlt.optimize.SGD.SGDPrm;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.DepParseFactorGraph.DepParseFactorGraphPrm;
import edu.jhu.srl.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.InformationGainFeatureTemplateSelectorPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.SrlFeatTemplates;
import edu.jhu.srl.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.srl.JointNlpFgExamplesBuilder.JointNlpFeatureExtractorPrm;
import edu.jhu.srl.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.tag.BrownClusterTagger;
import edu.jhu.tag.BrownClusterTagger.BrownClusterTaggerPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.files.Files;

/**
 * Pipeline runner for SRL experiments.
 * @author mgormley
 * @author mmitchell
 */
public class SrlRunner {

    public static enum InitParams { UNIFORM, RANDOM };
    
    public static enum Optimizer { LBFGS, SGD, ADAGRAD, ADADELTA };
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    @Opt(hasArg = true, description = "Whether to use a log-add table for faster computation.")
    public static boolean useLogAddTable = true;
    
    // Options for model IO
    @Opt(hasArg = true, description = "File from which to read a serialized model.")
    public static File modelIn = null;
    @Opt(hasArg = true, description = "File to which to serialize the model.")
    public static File modelOut = null;
    @Opt(hasArg = true, description = "File to which to print a human readable version of the model.")
    public static File printModel = null;

    // Options for initialization.
    @Opt(hasArg = true, description = "How to initialize the parameters of the model.")
    public static InitParams initParams = InitParams.UNIFORM;
    
    // Options for inference.
    @Opt(hasArg = true, description = "Whether to run inference in the log-domain.")
    public static boolean logDomain = true;
    @Opt(hasArg = true, description = "The BP schedule type.")
    public static BpScheduleType bpSchedule = BpScheduleType.TREE_LIKE;
    @Opt(hasArg = true, description = "The BP update order.")
    public static BpUpdateOrder bpUpdateOrder = BpUpdateOrder.SEQUENTIAL;
    @Opt(hasArg = true, description = "The max number of BP iterations.")
    public static int bpMaxIterations = 1;
    @Opt(hasArg = true, description = "Whether to normalize the messages.")
    public static boolean normalizeMessages = false;
    @Opt(hasArg = true, description = "The maximum message residual for convergence testing.")
    public static double bpConvergenceThreshold = 1e-3;
    
    // Options for dependency parse factor graph structure.
    @Opt(hasArg = true, description = "Whether to model the dependency parses.")
    public static boolean includeDp = true;
    @Opt(hasArg = true, description = "The type of the link variables.")
    public static VarType linkVarType = VarType.LATENT;
    @Opt(hasArg = true, description = "Whether to include a projective dependency tree global factor.")
    public static boolean useProjDepTreeFactor = false;
    @Opt(hasArg = true, description = "Whether to include 2nd-order grandparent factors in the model.")
    public static boolean grandparentFactors = false;
    @Opt(hasArg = true, description = "Whether to include 2nd-order sibling factors in the model.")
    public static boolean siblingFactors = false;
    @Opt(hasArg = true, description = "Whether to exclude non-projective grandparent factors.")
    public static boolean excludeNonprojectiveGrandparents = true;
    
    // Options for dependency parsing pruning.
    @Opt(hasArg = true, description = "File from which to read a first-order pruning model.")
    public static File pruneModel = null;
    @Opt(hasArg = true, description = "Whether to prune higher-order factors via a first-order pruning model.")
    public static boolean pruneByModel = false;
    @Opt(hasArg = true, description = "Whether to prune edges with a deterministic distance-based pruning approach.")
    public static boolean pruneByDist = false;
    
    // Options for Brown clusters.
    @Opt(hasArg = true, description = "Brown cluster file")
    public static File brownClusters = null;
    
    // Options for SRL factor graph structure.
    @Opt(hasArg = true, description = "Whether to model SRL.")
    public static boolean includeSrl = true;
    @Opt(hasArg = true, description = "The structure of the Role variables.")
    public static RoleStructure roleStructure = RoleStructure.PREDS_GIVEN;
    @Opt(hasArg = true, description = "Whether Role variables with unknown predicates should be latent.")
    public static boolean makeUnknownPredRolesLatent = true;
    @Opt(hasArg = true, description = "Whether to allow a predicate to assign a role to itself. (This should be turned on for English)")
    public static boolean allowPredArgSelfLoops = false;
    @Opt(hasArg = true, description = "Whether to include factors between the sense and role variables.")
    public static boolean binarySenseRoleFactors = false;
    @Opt(hasArg = true, description = "Whether to predict predicate sense.")
    public static boolean predictSense = false;
    @Opt(hasArg = true, description = "Whether to predict predicate positions.")
    public static boolean predictPredPos = false;

    // Options for joint factor graph structure.
    @Opt(hasArg = true, description = "Whether to include unary factors in the model.")
    public static boolean unaryFactors = false;

    // Options for SRL feature selection.
    @Opt(hasArg = true, description = "Whether to do feature selection.")
    public static boolean featureSelection = true;
    @Opt(hasArg = true, description = "The number of feature bigrams to form.")
    public static int numFeatsToSelect = 32;
    @Opt(hasArg = true, description = "The max number of sentences to use for feature selection")
    public static int numSentsForFeatSelect = 1000;    
    
    // Options for feature extraction.
    @Opt(hasArg = true, description = "For testing only: whether to use only the bias feature.")
    public static boolean biasOnly = false;
    @Opt(hasArg = true, description = "The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled.")
    public static int featureHashMod = 524288; // 2^19
    
    // Options for SRL feature extraction.
    @Opt(hasArg = true, description = "Cutoff for OOV words.")
    public static int cutoff = 3;
    @Opt(hasArg = true, description = "For preprocessing: Minimum feature count for caching.")
    public static int featCountCutoff = 4;
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
    
    // Options for Dependency parser feature extraction.
    @Opt(hasArg = true, description = "1st-order factor feature templates.")
    public static String dp1FeatTpls = TemplateSets.mcdonaldDepFeatsResource;
    @Opt(hasArg = true, description = "2nd-order factor feature templates.")
    public static String dp2FeatTpls = TemplateSets.carreras07Dep2FeatsResource;   
    @Opt(hasArg = true, description = "Whether to use SRL features for dep parsing.")
    public static boolean acl14DepFeats = true;
    
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
    @Opt(hasArg=true, description="Whether to automatically select the learning rate.")
    public static boolean sgdAutoSelectLr = true;
    @Opt(hasArg=true, description="How many epochs between auto-select runs.")
    public static int sgdAutoSelecFreq = 5;
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
            FastMath.useLogAddTable = useLogAddTable;
        }
        if (stopTrainingBy != null && new Date().after(stopTrainingBy)) {
            log.warn("Training will never begin since stopTrainingBy has already happened: " + stopTrainingBy);
            log.warn("Ignoring stopTrainingBy by setting it to null.");
            stopTrainingBy = null;
        }
        
        // Initialize the data reader/writer.
        CorpusHandler corpus = new CorpusHandler();
        
        // Get a model.
        JointNlpFgModel model = null;
        ObsFeatureConjoiner ofc;
        FactorTemplateList fts;
        CorpusStatistics cs;
        JointNlpFeatureExtractorPrm fePrm;
        PosTagDistancePruner ptdPruner = null;
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (JointNlpFgModel) Files.deserialize(modelIn);
            ofc = model.getOfc();
            fts = ofc.getTemplates();
            cs = model.getCs();
            fePrm = model.getFePrm();            
            // TODO: use atList here.
        } else {
            fePrm = getJointNlpFeatureExtractorPrm();
            removeAts(fePrm);
            cs = new CorpusStatistics(getCorpusStatisticsPrm());
            featureSelection(corpus.getTrainGold(), cs, fePrm);
            fts = new FactorTemplateList();
            ofc = new ObsFeatureConjoiner(getObsFeatureConjoinerPrm(), fts);
        }

        if (corpus.hasTrain()) {
            String name = "train";            
            SimpleAnnoSentenceCollection goldSents = corpus.getTrainGold();
            SimpleAnnoSentenceCollection inputSents = corpus.getTrainInput();

            addBrownClusters(inputSents);
            SimpleAnnoSentenceCollection.copyShallow(inputSents, goldSents, AT.BROWN);
            // Train the distance-based pruner. 
            if (pruneByDist) {
                ptdPruner = new PosTagDistancePruner();
                ptdPruner.train(goldSents);
            }
            addPruneMask(inputSents, ptdPruner, name);
            // Ensure that the gold data is annotated with the pruning mask as well.
            SimpleAnnoSentenceCollection.copyShallow(inputSents, goldSents, AT.DEP_EDGE_MASK);
            printOracleAccuracyAfterPruning(inputSents, goldSents, "train");

            // Train a model.
            FgExampleList data = getData(ofc, cs, name, goldSents, fePrm, true);
            
            if (model == null) {
                model = new JointNlpFgModel(cs, ofc, fePrm);
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
            SimpleAnnoSentenceCollection predSents = decode(model, data, inputSents, name);
            corpus.writeTrainPreds(predSents);
            eval(name, goldSents, predSents);
            corpus.clearTrainCache();
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
            SimpleAnnoSentenceCollection inputSents = corpus.getDevInput();
            addBrownClusters(inputSents);
            addPruneMask(inputSents, ptdPruner, name);
            FgExampleList data = getData(ofc, cs, name, inputSents, fePrm, false);
            // Decode and evaluate the dev data.
            SimpleAnnoSentenceCollection predSents = decode(model, data, inputSents, name);            
            corpus.writeDevPreds(predSents);
            SimpleAnnoSentenceCollection goldSents = corpus.getDevGold();
            printOracleAccuracyAfterPruning(inputSents, goldSents, "dev");
            eval(name, goldSents, predSents);
            corpus.clearDevCache();
        }
        
        if (corpus.hasTest()) {
            // Test the model on test data.
            fts.stopGrowth();
            String name = "test";
            SimpleAnnoSentenceCollection inputSents = corpus.getTestInput();
            addBrownClusters(inputSents);
            addPruneMask(inputSents, ptdPruner, name);
            FgExampleList data = getData(ofc, cs, name, inputSents, fePrm, false);
            // Decode and evaluate the test data.
            SimpleAnnoSentenceCollection predSents = decode(model, data, inputSents, name);
            corpus.writeTestPreds(predSents);
            SimpleAnnoSentenceCollection goldSents = corpus.getTestGold();
            printOracleAccuracyAfterPruning(inputSents, goldSents, "test");
            eval(name, goldSents, predSents);
            corpus.clearTestCache();
        }
    }

    private void addBrownClusters(SimpleAnnoSentenceCollection sents) throws IOException {
        if (brownClusters != null) {            
            log.info("Adding Brown clusters.");
            BrownClusterTagger bct = new BrownClusterTagger(getBrownCluterTaggerPrm());
            bct.read(brownClusters);
            bct.annotate(sents);
            log.info("Brown cluster hit rate: " + bct.getHitRate());
        } else {
            log.warn("No Brown cluster file specified.");            
        }
    }

    private void addPruneMask(SimpleAnnoSentenceCollection inputSents, PosTagDistancePruner ptdPruner, String name) {
        if (pruneByDist) {
            // Prune via the distance-based pruner.
            ptdPruner.annotate(inputSents);
        }
        if (pruneByModel) {
            if (pruneModel == null) {
                throw new IllegalStateException("If pruneEdges is true, pruneModel must be specified.");
            }
            FirstOrderPruner foPruner = new FirstOrderPruner(pruneModel, getSrlFgExampleBuilderPrm(null), getDecoderPrm());
            foPruner.annotate(inputSents);
        }
    }

    private void printOracleAccuracyAfterPruning(SimpleAnnoSentenceCollection predSents, SimpleAnnoSentenceCollection goldSents, String name) {
        if (pruneByDist || pruneByModel) {
            int numTot = 0;
            int numCorrect = 0;
            for (int i=0; i<predSents.size(); i++) {
                SimpleAnnoSentence predSent = predSents.get(i);
                SimpleAnnoSentence goldSent = goldSents.get(i);
                if (predSent.getDepEdgeMask() != null) {
                    for (int c=0; c<goldSent.size(); c++) {
                        int p = goldSent.getParent(c);
                        if (predSent.getDepEdgeMask().isKept(p, c)) {
                            numCorrect++;
                        }
                        numTot++;
                    }
                }
            }
            log.info("Oracle pruning accuracy on " + name + ": " + (double) numCorrect / numTot);
        }
    }
    
    /**
     * Do feature selection and update fePrm with the chosen feature templates.
     */
    private void featureSelection(SimpleAnnoSentenceCollection sents, CorpusStatistics cs, JointNlpFeatureExtractorPrm fePrm) throws IOException,
            ParseException {
        SrlFeatureExtractorPrm srlFePrm = fePrm.srlFePrm;
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
            removeAts(fePrm); // TODO: This probably isn't necessary, but just in case.
        }
        if (includeSrl && acl14DepFeats) {
            fePrm.dpFePrm.firstOrderTpls = srlFePrm.fePrm.pairTemplates;            
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

    private void removeAts(JointNlpFeatureExtractorPrm fePrm) {
        List<AT> ats = Lists.union(CorpusHandler.getRemoveAts(), CorpusHandler.getPredAts());
        if (brownClusters == null) {
            // Filter out the Brown cluster features.
            log.warn("Filtering out Brown cluster features.");
            ats.add(AT.BROWN);
        }
        for (AT at : ats) {
            fePrm.srlFePrm.fePrm.soloTemplates = TemplateLanguage.filterOutRequiring(fePrm.srlFePrm.fePrm.soloTemplates, at);
            fePrm.srlFePrm.fePrm.pairTemplates   = TemplateLanguage.filterOutRequiring(fePrm.srlFePrm.fePrm.pairTemplates, at);
            fePrm.dpFePrm.firstOrderTpls = TemplateLanguage.filterOutRequiring(fePrm.dpFePrm.firstOrderTpls, at);
            fePrm.dpFePrm.secondOrderTpls   = TemplateLanguage.filterOutRequiring(fePrm.dpFePrm.secondOrderTpls, at);
        }
    }

    private FgExampleList getData(ObsFeatureConjoiner ofc, CorpusStatistics cs, String name,
            SimpleAnnoSentenceCollection sents, JointNlpFeatureExtractorPrm fePrm, boolean labeledExamples) {
        JointNlpFgExampleBuilderPrm prm = getSrlFgExampleBuilderPrm(fePrm);        
        return getData(ofc, cs, name, sents, fePrm, prm, labeledExamples);
    }

    private static FgExampleList getData(ObsFeatureConjoiner ofc, CorpusStatistics cs, String name,
            SimpleAnnoSentenceCollection sents, JointNlpFeatureExtractorPrm fePrm, JointNlpFgExampleBuilderPrm prm,
            boolean labeledExamples) {        
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm, ofc, cs, labeledExamples);
        FgExampleList data = builder.getData(sents);
        return data;
    }

    private static void printPredArgSelfLoopStats(SimpleAnnoSentenceCollection sents) {
        int numPredArgSelfLoop = 0;
        int numPredArgs = 0;
        for (SimpleAnnoSentence sent : sents) {
            if (sent.getSrlGraph() != null) {
                for (SrlEdge edge : sent.getSrlGraph().getEdges()) {
                    if (edge.getArg().getPosition() == edge.getPred().getPosition()) {
                        numPredArgSelfLoop += 1;
                    }
                }
                numPredArgs += sent.getSrlGraph().getEdges().size();
            }
        }
        if (numPredArgs > 0) {
            log.info(String.format("Proportion pred-arg self loops: %.4f (%d / %d)", (double) numPredArgSelfLoop/numPredArgs, numPredArgSelfLoop, numPredArgs));
        }
    }

    private void eval(String name, SimpleAnnoSentenceCollection goldSents, SimpleAnnoSentenceCollection predSents) {
        printPredArgSelfLoopStats(goldSents);

        DepParseEvaluator eval = new DepParseEvaluator(name);
        eval.evaluate(goldSents, predSents);
    }
    
    private void eval(String name, VarConfigPair pair) {
        AccuracyEvaluator accEval = new AccuracyEvaluator();
        double accuracy = accEval.evaluate(pair.gold, pair.pred);
        log.info(String.format("Accuracy on %s: %.6f", name, accuracy));
    }
    
    private SimpleAnnoSentenceCollection decode(FgModel model, FgExampleList data, SimpleAnnoSentenceCollection inputSents, String name) throws IOException, ParseException {
        log.info("Running the decoder on " + name + " data.");

        Timer timer = new Timer();
        timer.start();
        // Add the new predictions to the input sentences.
        for (int i = 0; i < inputSents.size(); i++) {
            UFgExample ex = data.get(i);
            SimpleAnnoSentence predSent = inputSents.get(i);
            JointNlpDecoder decoder = new JointNlpDecoder(getDecoderPrm());
            decoder.decode(model, ex);
                        
            // Update SRL graph on the sentence. 
            SrlGraph srlGraph = decoder.getSrlGraph();
            if (srlGraph != null) {
                predSent.setSrlGraph(srlGraph);
            }
            // Update the dependency tree on the sentence.
            int[] parents = decoder.getParents();
            if (parents != null) {
                predSent.setParents(parents);
            }
        }
        timer.stop();
        log.info(String.format("Decoded %s at %.2f tokens/sec", name, inputSents.getNumTokens() / timer.totSec()));
        
        return inputSents;
    }

    

    /* --------- Factory Methods ---------- */
    
    private static JointNlpFgExampleBuilderPrm getSrlFgExampleBuilderPrm(JointNlpFeatureExtractorPrm fePrm) {
        JointNlpFgExampleBuilderPrm prm = new JointNlpFgExampleBuilderPrm();
        
        // Factor graph structure.
        prm.fgPrm.dpPrm.linkVarType = linkVarType;
        prm.fgPrm.dpPrm.useProjDepTreeFactor = useProjDepTreeFactor;
        prm.fgPrm.dpPrm.unaryFactors = unaryFactors;
        prm.fgPrm.dpPrm.excludeNonprojectiveGrandparents = excludeNonprojectiveGrandparents;
        prm.fgPrm.dpPrm.grandparentFactors = grandparentFactors;
        prm.fgPrm.dpPrm.siblingFactors = siblingFactors;
        prm.fgPrm.dpPrm.pruneEdges = pruneByDist || pruneByModel;
                
        prm.fgPrm.srlPrm.makeUnknownPredRolesLatent = makeUnknownPredRolesLatent;
        prm.fgPrm.srlPrm.roleStructure = roleStructure;
        prm.fgPrm.srlPrm.allowPredArgSelfLoops = allowPredArgSelfLoops;
        prm.fgPrm.srlPrm.unaryFactors = unaryFactors;
        prm.fgPrm.srlPrm.binarySenseRoleFactors = binarySenseRoleFactors;
        prm.fgPrm.srlPrm.predictSense = predictSense;
        prm.fgPrm.srlPrm.predictPredPos = predictPredPos;
        
        prm.fgPrm.includeDp = includeDp;
        prm.fgPrm.includeSrl = includeSrl;
        
        // Feature extraction.
        prm.fePrm = fePrm;
        
        // Example construction and storage.
        prm.exPrm.cacheType = cacheType;
        prm.exPrm.gzipped = gzipCache;
        prm.exPrm.maxEntriesInMemory = maxEntriesInMemory;
        
        return prm;
    }
    
    private static ObsFeatureConjoinerPrm getObsFeatureConjoinerPrm() {
        ObsFeatureConjoinerPrm prm = new ObsFeatureConjoinerPrm();
        prm.featCountCutoff = featCountCutoff;
        prm.includeUnsupportedFeatures = includeUnsupportedFeatures;
        return prm;
    }
    
    private static JointNlpFeatureExtractorPrm getJointNlpFeatureExtractorPrm() {
        // SRL Feature Extraction.
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

        srlFePrm.featureHashMod = featureHashMod;
                
        // Dependency parsing Feature Extraction
        DepParseFeatureExtractorPrm dpFePrm = new DepParseFeatureExtractorPrm();
        dpFePrm.biasOnly = biasOnly;
        dpFePrm.firstOrderTpls = getFeatTpls(dp1FeatTpls);
        dpFePrm.secondOrderTpls = getFeatTpls(dp2FeatTpls);
        dpFePrm.featureHashMod = featureHashMod;
        if (includeSrl && acl14DepFeats) {
            // This special case is only for historical consistency.
            dpFePrm.onlyTrueBias = false;
            dpFePrm.onlyTrueEdges = false;
        }
        
        JointNlpFeatureExtractorPrm fePrm = new JointNlpFeatureExtractorPrm();
        fePrm.srlFePrm = srlFePrm;
        fePrm.dpFePrm = dpFePrm;
        return fePrm;
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
        } else if (optimizer == Optimizer.SGD || optimizer == Optimizer.ADAGRAD || optimizer == Optimizer.ADADELTA) {
            prm.maximizer = null;
            SGDPrm sgdPrm = getSgdPrm();
            if (optimizer == Optimizer.SGD){
                BottouSchedulePrm boPrm = new BottouSchedulePrm();
                boPrm.initialLr = sgdInitialLr;
                boPrm.lambda = 1.0 / l2variance;
                sgdPrm.sched = new BottouSchedule(boPrm);
            } else if (optimizer == Optimizer.ADAGRAD){
                AdaGradPrm adaGradPrm = new AdaGradPrm();
                adaGradPrm.eta = adaGradEta;
                sgdPrm.sched = new AdaGrad(adaGradPrm);
            } else if (optimizer == Optimizer.ADADELTA){
                AdaDeltaPrm adaDeltaPrm = new AdaDeltaPrm();
                adaDeltaPrm.decayRate = adaDeltaDecayRate;
                adaDeltaPrm.constantAddend = adaDeltaConstantAddend;
                sgdPrm.sched = new AdaDelta(adaDeltaPrm);
                sgdPrm.autoSelectLr = false;
            }
            prm.batchMaximizer = new SGD(sgdPrm);
        } else {
            throw new RuntimeException("Optimizer not supported: " + optimizer);
        }
        prm.regularizer = new L2(l2variance);
        prm.numThreads = threads;
        return prm;
    }

    private static edu.jhu.hlt.optimize.Optimizer<DifferentiableFunction> getLbfgs() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        prm.maxIterations = maxLbfgsIterations;
        return new MalletLBFGS(prm);
    }
    
    private static SGDPrm getSgdPrm() {
        SGDPrm prm = new SGDPrm();
        prm.numPasses = sgdNumPasses;
        prm.batchSize = sgdBatchSize;
        prm.withReplacement = sgdWithRepl;
        prm.stopBy = stopTrainingBy;
        prm.autoSelectLr = sgdAutoSelectLr;
        prm.autoSelectFreq = sgdAutoSelecFreq;
        // Make sure we correctly set the schedule somewhere else.
        prm.sched = null;
        return prm;
    }

    private static BeliefPropagationPrm getInfFactory() {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = logDomain;        
        bpPrm.schedule = bpSchedule;
        bpPrm.updateOrder = bpUpdateOrder;
        bpPrm.normalizeMessages = normalizeMessages;
        bpPrm.maxIterations = bpMaxIterations;
        bpPrm.convergenceThreshold = bpConvergenceThreshold;
        return bpPrm;
    }

    private static JointNlpDecoderPrm getDecoderPrm() {
        MbrDecoderPrm mbrPrm = new MbrDecoderPrm();
        mbrPrm.infFactory = getInfFactory();
        mbrPrm.loss = Loss.ACCURACY;
        JointNlpDecoderPrm prm = new JointNlpDecoderPrm();
        prm.mbrPrm = mbrPrm;
        return prm;
    }

    private static BrownClusterTaggerPrm getBrownCluterTaggerPrm() {
        BrownClusterTaggerPrm bcPrm = new BrownClusterTaggerPrm();
        bcPrm.language = CorpusHandler.language;
        return bcPrm;
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
