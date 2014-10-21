package edu.jhu.nlp.joint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.autodiff.erma.DepParseDecodeLoss.DepParseDecodeLossFactory;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.autodiff.erma.ErmaObjective.BeliefsModuleFactory;
import edu.jhu.autodiff.erma.ExpectedRecall.ExpectedRecallFactory;
import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.autodiff.erma.MeanSquaredError.MeanSquaredErrorFactory;
import edu.jhu.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.gm.decode.MbrDecoder.Loss;
import edu.jhu.gm.decode.MbrDecoder.MbrDecoderPrm;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.gm.train.CrfTrainer.Trainer;
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
import edu.jhu.hlt.optimize.StanfordQNMinimizer;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.nlp.AnnoPipeline;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.EvalPipeline;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.InformationGainFeatureTemplateSelector;
import edu.jhu.nlp.InformationGainFeatureTemplateSelector.InformationGainFeatureTemplateSelectorPrm;
import edu.jhu.nlp.InformationGainFeatureTemplateSelector.SrlFeatTemplates;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.depparse.FirstOrderPruner;
import edu.jhu.nlp.depparse.PosTagDistancePruner;
import edu.jhu.nlp.embed.Embeddings.Scaling;
import edu.jhu.nlp.embed.EmbeddingsAnnotator;
import edu.jhu.nlp.embed.EmbeddingsAnnotator.EmbeddingsAnnotatorPrm;
import edu.jhu.nlp.eval.DepParseEvaluator;
import edu.jhu.nlp.eval.OraclePruningAccuracy;
import edu.jhu.nlp.eval.RelationEvaluator;
import edu.jhu.nlp.eval.SrlSelfLoops;
import edu.jhu.nlp.features.TemplateLanguage;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.features.TemplateReader;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.nlp.features.TemplateWriter;
import edu.jhu.nlp.joint.JointNlpAnnotator.InitParams;
import edu.jhu.nlp.joint.JointNlpAnnotator.JointNlpAnnotatorPrm;
import edu.jhu.nlp.joint.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.nlp.relations.RelationsOptions;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleStructure;
import edu.jhu.nlp.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.nlp.tag.BrownClusterTagger;
import edu.jhu.nlp.tag.BrownClusterTagger.BrownClusterTaggerPrm;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.report.Reporter;
import edu.jhu.util.report.ReporterManager;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

/**
 * Pipeline runner for SRL experiments.
 * @author mgormley
 * @author mmitchell
 */
public class JointNlpRunner {

    public static enum Optimizer { LBFGS, QN, SGD, ADAGRAD, ADADELTA };

    public enum ErmaLoss { MSE, EXPECTED_RECALL, DP_DECODE_LOSS };

    public enum Inference { BRUTE_FORCE, BP };
    
    public enum RegularizerType { L2, NONE };
    
    public enum AlgebraType {
        REAL(Algebras.REAL_ALGEBRA), LOG(Algebras.LOG_SEMIRING), LOG_SIGN(Algebras.LOG_SIGN_ALGEBRA),
        // SHIFTED_REAL and SPLIT algebras are for testing only.
        SHIFTED_REAL(Algebras.SHIFTED_REAL_ALGEBRA), SPLIT(Algebras.SPLIT_ALGEBRA);

        private Algebra s;
        
        private AlgebraType(Algebra s) {
            this.s = s;
        }
        
        public Algebra getAlgebra() {
            return s;
        }
    }

    private static final Logger log = Logger.getLogger(JointNlpRunner.class);
    private static final Reporter rep = Reporter.getReporter(JointNlpRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    @Opt(hasArg = true, description = "Whether to use a log-add table for faster computation.")
    public static boolean useLogAddTable = false;
    
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
    @Opt(hasArg = true, description = "Type of inference method.")
    public static Inference inference = Inference.BP;
    @Opt(hasArg = true, description = "Whether to run inference in the log-domain.")
    public static AlgebraType algebra = AlgebraType.REAL;
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
    @Opt(hasArg = true, description = "Directory to dump debugging information for BP.")
    public static File bpDumpDir = null;
    
    // Options for Brown clusters.
    @Opt(hasArg = true, description = "Brown cluster file")
    public static File brownClusters = null;
    
    // Options for Embeddings.
    @Opt(hasArg=true, description="Path to word embeddings text file.")
    public static File embeddingsFile = null;
    @Opt(hasArg=true, description="Method for normalization of the embeddings.")
    public static Scaling embNorm = Scaling.L1_NORM;
    @Opt(hasArg=true, description="Amount to scale embeddings after normalization.")
    public static double embScaler = 15.0;
    
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

    // Options for Dependency parser feature extraction.
    @Opt(hasArg = true, description = "1st-order factor feature templates.")
    public static String dp1FeatTpls = TemplateSets.mcdonaldDepFeatsResource;
    @Opt(hasArg = true, description = "2nd-order factor feature templates.")
    public static String dp2FeatTpls = TemplateSets.carreras07Dep2FeatsResource;   
    @Opt(hasArg = true, description = "Whether to use SRL features for dep parsing.")
    public static boolean acl14DepFeats = true;
    @Opt(hasArg = true, description = "Whether to use the fast feature set for dep parsing.")
    public static boolean dpFastFeats = true;
    
    // Options for relation extraction.
    @Opt(hasArg = true, description = "Whether to model Relation Extraction.")
    public static boolean includeRel = false;
    @Opt(hasArg = true, description = "Relation feature templates.")
    public static String relFeatTpls = null;
    
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
    
    // Options for optimization.
    @Opt(hasArg=true, description="The optimization method to use for training.")
    public static Optimizer optimizer = Optimizer.LBFGS;
    @Opt(hasArg=true, description="The variance for the L2 regularizer.")
    public static double l2variance = 1.0;
    @Opt(hasArg=true, description="The type of regularizer.")
    public static RegularizerType regularizer = RegularizerType.L2;
    @Opt(hasArg=true, description="Max iterations for L-BFGS training.")
    public static int maxLbfgsIterations = 1000;
    @Opt(hasArg=true, description="Number of effective passes over the dataset for SGD.")
    public static int sgdNumPasses = 30;
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
    @Opt(hasArg=true, description="Whether to compute the function value on iterations other than the last.")
    public static boolean sgdComputeValueOnNonFinalIter = true;
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
    
    // Options for training.
    @Opt(hasArg=true, description="Whether to use the mean squared error instead of conditional log-likelihood when evaluating training quality.")
    public static boolean useMseForValue = false;
    @Opt(hasArg=true, description="The type of trainer to use (e.g. conditional log-likelihood, ERMA).")
    public static Trainer trainer = Trainer.CLL;
    
    // Options for training a dependency parser with ERMA.
    @Opt(hasArg=true, description="The start temperature for the softmax MBR decoder for dependency parsing.")
    public static double dpStartTemp = 10;
    @Opt(hasArg=true, description="The end temperature for the softmax MBR decoder for dependency parsing.")
    public static double dpEndTemp = .1;
    @Opt(hasArg=true, description="Whether to transition from MSE to the softmax MBR decoder with expected recall.")
    public static boolean dpAnnealMse = true;
    @Opt(hasArg=true, description="Whether to transition from MSE to the softmax MBR decoder with expected recall.")
    public static ErmaLoss dpLoss = ErmaLoss.DP_DECODE_LOSS;
    
    public JointNlpRunner() {
    }

    public void run() throws ParseException, IOException {  
        Timer t = new Timer();
        t.start();
        FastMath.useLogAddTable = useLogAddTable;
        if (useLogAddTable) {
            log.warn("Using log-add table instead of exact computation. When using global factors, this may result in numerical instability.");
        }
        if (stopTrainingBy != null && new Date().after(stopTrainingBy)) {
            log.warn("Training will never begin since stopTrainingBy has already happened: " + stopTrainingBy);
            log.warn("Ignoring stopTrainingBy by setting it to null.");
            stopTrainingBy = null;
        }
        
        // Initialize the data reader/writer.
        CorpusHandler corpus = new CorpusHandler();
        
        // Get a model.
        if (modelIn == null && !corpus.hasTrain()) {
        	throw new ParseException("Either --modelIn or --train must be specified.");
        }
        JointNlpAnnotatorPrm prm = getJointNlpAnnotatorPrm();
        if (modelIn == null && corpus.hasTrain()) {
            // Feature selection.
            featureSelection(corpus.getTrainGold(), prm.buPrm.fePrm);
        }
        
        AnnoPipeline anno = new AnnoPipeline();
        EvalPipeline eval = new EvalPipeline();
        JointNlpAnnotator jointAnno = new JointNlpAnnotator(prm);
        if (modelIn != null) {
            jointAnno.loadModel(modelIn);
        }
        {
            // Add Brown clusters.
            if (brownClusters != null) {      
                anno.add(new Annotator() {                    
                    @Override
                    public void annotate(AnnoSentenceCollection sents) {
                        log.info("Adding Brown clusters.");
                        BrownClusterTagger bct = new BrownClusterTagger(getBrownCluterTaggerPrm());
                        bct.read(brownClusters);
                        bct.annotate(sents);
                        log.info("Brown cluster hit rate: " + bct.getHitRate());                        
                    }
                });
            } else {
                log.info("No Brown clusters file specified.");
            }
            // Add word embeddings.
            if (embeddingsFile != null) {
                anno.add(new Annotator() {                   
                    @Override
                    public void annotate(AnnoSentenceCollection sents) {
                        log.info("Adding word embeddings.");
                        EmbeddingsAnnotator ea = new EmbeddingsAnnotator(getEmbeddingsAnnotatorPrm());
                        ea.annotate(sents);
                        log.info("Embeddings hit rate: " + ea.getHitRate());                        
                    }
                });
            } else {
                log.info("No embeddings file specified.");
            }
            
            if (pruneByDist) {
                // Prune via the distance-based pruner.
                anno.add(new PosTagDistancePruner());
            }
            if (pruneByModel) {
                if (pruneModel == null) {
                    throw new IllegalStateException("If pruneEdges is true, pruneModel must be specified.");
                }
                anno.add(new FirstOrderPruner(pruneModel, getSrlFgExampleBuilderPrm(null), getDecoderPrm()));
            }
            
            // Various NLP annotations.
            anno.add(jointAnno);
        }
        {
            if (pruneByDist || pruneByModel) {
                eval.add(new OraclePruningAccuracy());
            }
            eval.add(new SrlSelfLoops());
            eval.add(new DepParseEvaluator());
            eval.add(new RelationEvaluator());
        }
        
        if (corpus.hasTrain()) {
            String name = "train";            
            AnnoSentenceCollection trainGold = corpus.getTrainGold();
            AnnoSentenceCollection trainInput = corpus.getTrainInput();
            // (Dev data might be null.)
            AnnoSentenceCollection devGold = corpus.getDevGold();
            AnnoSentenceCollection devInput = corpus.getDevInput();
            
            // Train a model. (The PipelineAnnotator also annotates all the input.)
            anno.train(trainInput, trainGold);
            
            // Decode and evaluate the train data.
            corpus.writeTrainPreds(trainInput);
            eval.evaluate(trainInput, trainGold, name);
            corpus.clearTrainCache();

            if (corpus.hasDev()) {
                // Write dev data predictions.
                name = "dev";
                corpus.writeDevPreds(devInput);
                // Evaluate dev data.
                eval.evaluate(devInput, devGold, name);
                corpus.clearDevCache();
            }
        }
        
        if (modelOut != null) {
            jointAnno.saveModel(modelOut);
        }
        if (printModel != null) {
            jointAnno.printModel(printModel);
        }
        
        if (corpus.hasTest()) {
            // Decode test data.
            String name = "test";
            AnnoSentenceCollection testInput = corpus.getTestInput();
            anno.annotate(testInput);
            corpus.writeTestPreds(testInput);
            // Evaluate test data.
            AnnoSentenceCollection testGold = corpus.getTestGold();
            eval.evaluate(testInput, testGold, name);
            corpus.clearTestCache();
        }
        t.stop();
        rep.report("elapsedSec", t.totSec());
    }

    /**
     * Do feature selection and update fePrm with the chosen feature templates.
     */
    private void featureSelection(AnnoSentenceCollection sents, JointNlpFeatureExtractorPrm fePrm) throws IOException,
            ParseException {
        SrlFeatureExtractorPrm srlFePrm = fePrm.srlFePrm;
        // Remove annotation types from the features which are explicitly excluded.
        removeAts(fePrm);
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

    private void removeAts(JointNlpEncoder.JointNlpFeatureExtractorPrm fePrm) {
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
    
    /* --------- Factory Methods ---------- */

    private static JointNlpAnnotatorPrm getJointNlpAnnotatorPrm() throws ParseException {
        JointNlpAnnotatorPrm prm = new JointNlpAnnotatorPrm();
        prm.crfPrm = getCrfTrainerPrm();
        prm.csPrm = getCorpusStatisticsPrm();
        prm.dePrm = getDecoderPrm();
        prm.initParams = initParams;
        prm.ofcPrm = getObsFeatureConjoinerPrm();
        JointNlpFeatureExtractorPrm fePrm = getJointNlpFeatureExtractorPrm();
        prm.buPrm = getSrlFgExampleBuilderPrm(fePrm);
        return prm;
    }
    
    private static JointNlpFgExampleBuilderPrm getSrlFgExampleBuilderPrm(JointNlpEncoder.JointNlpFeatureExtractorPrm fePrm) {
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
        
        // Relation Feature extraction.
        if (includeRel) {
            if (relFeatTpls != null) { prm.fgPrm.relPrm.templates = getFeatTpls(relFeatTpls); }
            prm.fgPrm.relPrm.featureHashMod = featureHashMod;
        }
        
        prm.fgPrm.includeDp = includeDp;
        prm.fgPrm.includeSrl = includeSrl;
        prm.fgPrm.includeRel = includeRel;
        
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
        dpFePrm.onlyFast = dpFastFeats;
        
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
    
    private static CrfTrainerPrm getCrfTrainerPrm() throws ParseException {
        FgInferencerFactory infPrm = getInfFactory();
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = infPrm;
        if (infPrm instanceof BeliefsModuleFactory) {
            // TODO: This is a temporary hack to which assumes we always use ErmaBp.
            prm.bFactory = (BeliefsModuleFactory) infPrm;
        }
        if (optimizer == Optimizer.LBFGS) {
            prm.optimizer = getMalletLbfgs();
            prm.batchOptimizer = null;
        } else if (optimizer == Optimizer.QN) {
            prm.optimizer = getStanfordLbfgs();
            prm.batchOptimizer = null;            
        } else if (optimizer == Optimizer.SGD || optimizer == Optimizer.ADAGRAD || optimizer == Optimizer.ADADELTA) {
            prm.optimizer = null;
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
            prm.batchOptimizer = new SGD(sgdPrm);
        } else {
            throw new RuntimeException("Optimizer not supported: " + optimizer);
        }
        if (regularizer == RegularizerType.L2) {
            prm.regularizer = new L2(l2variance);
        } else if (regularizer == RegularizerType.NONE) {
            prm.regularizer = null;
        } else {
            throw new ParseException("Unsupported regularizer: " + regularizer);
        }
        prm.numThreads = threads;
        prm.useMseForValue = useMseForValue;        
        prm.trainer = trainer;                
        
        // TODO: add options for other loss functions.
        if (prm.trainer == Trainer.ERMA && includeDp && !includeSrl) {
            if (dpLoss == ErmaLoss.DP_DECODE_LOSS) {
                DepParseDecodeLossFactory lossPrm = new DepParseDecodeLossFactory();
                lossPrm.annealMse = dpAnnealMse;
                lossPrm.startTemp = dpStartTemp;
                lossPrm.endTemp = dpEndTemp;
                prm.dlFactory = lossPrm;
            } else if (dpLoss == ErmaLoss.MSE) {
                prm.dlFactory = new MeanSquaredErrorFactory();
            } else if (dpLoss == ErmaLoss.EXPECTED_RECALL) {
                prm.dlFactory = new ExpectedRecallFactory();
            }
        }
        
        return prm;
    }

    private static edu.jhu.hlt.optimize.Optimizer<DifferentiableFunction> getMalletLbfgs() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        prm.maxIterations = maxLbfgsIterations;
        return new MalletLBFGS(prm);
    }

    private static edu.jhu.hlt.optimize.Optimizer<DifferentiableFunction> getStanfordLbfgs() {
        return new StanfordQNMinimizer(maxLbfgsIterations);
    }
    
    private static SGDPrm getSgdPrm() {
        SGDPrm prm = new SGDPrm();
        prm.numPasses = sgdNumPasses;
        prm.batchSize = sgdBatchSize;
        prm.withReplacement = sgdWithRepl;
        prm.stopBy = stopTrainingBy;
        prm.autoSelectLr = sgdAutoSelectLr;
        prm.autoSelectFreq = sgdAutoSelecFreq;
        prm.computeValueOnNonFinalIter = sgdComputeValueOnNonFinalIter;
        // Make sure we correctly set the schedule somewhere else.
        prm.sched = null;
        return prm;
    }

    private static FgInferencerFactory getInfFactory() throws ParseException {
        if (inference == Inference.BRUTE_FORCE) {
            BruteForceInferencerPrm prm = new BruteForceInferencerPrm(algebra.getAlgebra());
            return prm;
        } else if (inference == Inference.BP) {
            ErmaBpPrm bpPrm = new ErmaBpPrm();
            bpPrm.logDomain = logDomain;
            bpPrm.s = algebra.getAlgebra();
            bpPrm.schedule = bpSchedule;
            bpPrm.updateOrder = bpUpdateOrder;
            bpPrm.normalizeMessages = normalizeMessages;
            bpPrm.maxIterations = bpMaxIterations;
            bpPrm.convergenceThreshold = bpConvergenceThreshold;
            bpPrm.keepTape = (trainer == Trainer.ERMA);
            if (bpDumpDir != null) {
                bpPrm.dumpDir = Paths.get(bpDumpDir.getAbsolutePath());
            }
            return bpPrm;
        } else {
            throw new ParseException("Unsupported inference method: " + inference);
        }
    }

    private static JointNlpDecoderPrm getDecoderPrm() throws ParseException {
        MbrDecoderPrm mbrPrm = new MbrDecoderPrm();
        mbrPrm.infFactory = getInfFactory();
        mbrPrm.loss = Loss.L1;
        JointNlpDecoderPrm prm = new JointNlpDecoderPrm();
        prm.mbrPrm = mbrPrm;
        return prm;
    }

    private static BrownClusterTaggerPrm getBrownCluterTaggerPrm() {
        BrownClusterTaggerPrm bcPrm = new BrownClusterTaggerPrm();
        bcPrm.language = CorpusHandler.language;
        return bcPrm;
    }

    private static EmbeddingsAnnotatorPrm getEmbeddingsAnnotatorPrm() {
        EmbeddingsAnnotatorPrm prm = new EmbeddingsAnnotatorPrm();
        prm.embeddingsFile = embeddingsFile;
        prm.embNorm = embNorm;
        prm.embScaler= embScaler;
        return prm;
    }
    
    public static void main(String[] args) {
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(JointNlpRunner.class);
            parser.addClass(JointNlpRunner.class);
            parser.addClass(CorpusHandler.class);
            parser.addClass(RelationsOptions.class);
            parser.addClass(InsideOutsideDepParse.class);      
            parser.addClass(ReporterManager.class);
            parser.parseArgs(args);
            
            ReporterManager.init(ReporterManager.reportOut, true);
            Prng.seed(seed);

            JointNlpRunner pipeline = new JointNlpRunner();
            pipeline.run();
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            if (parser != null) {
                parser.printUsage();
            }
            exitCode = 1;
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            ReporterManager.close();
        }
        
        System.exit(exitCode);
    }

}
