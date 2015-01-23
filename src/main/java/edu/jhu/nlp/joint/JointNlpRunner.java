package edu.jhu.nlp.joint;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import edu.jhu.hlt.optimize.AdaGradComidL2;
import edu.jhu.hlt.optimize.AdaGradComidL2.AdaGradComidL2Prm;
import edu.jhu.hlt.optimize.AdaGradSchedule;
import edu.jhu.hlt.optimize.AdaGradSchedule.AdaGradSchedulePrm;
import edu.jhu.hlt.optimize.BottouSchedule;
import edu.jhu.hlt.optimize.BottouSchedule.BottouSchedulePrm;
import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.SGD;
import edu.jhu.hlt.optimize.SGD.SGDPrm;
import edu.jhu.hlt.optimize.SGDFobos;
import edu.jhu.hlt.optimize.SGDFobos.SGDFobosPrm;
import edu.jhu.hlt.optimize.StanfordQNMinimizer;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.nlp.AnnoPipeline;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.EvalPipeline;
import edu.jhu.nlp.TransientAnnotator;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor.DepParseFeatureExtractorPrm;
import edu.jhu.nlp.depparse.FirstOrderPruner;
import edu.jhu.nlp.depparse.GoldDepParseUnpruner;
import edu.jhu.nlp.depparse.O2AllGraFgInferencer.O2AllGraFgInferencerFactory;
import edu.jhu.nlp.depparse.PosTagDistancePruner;
import edu.jhu.nlp.embed.Embeddings.Scaling;
import edu.jhu.nlp.embed.EmbeddingsAnnotator;
import edu.jhu.nlp.embed.EmbeddingsAnnotator.EmbeddingsAnnotatorPrm;
import edu.jhu.nlp.eval.DepParseAccuracy;
import edu.jhu.nlp.eval.DepParseExactMatch;
import edu.jhu.nlp.eval.OraclePruningAccuracy;
import edu.jhu.nlp.eval.OraclePruningExactMatch;
import edu.jhu.nlp.eval.ProportionAnnotated;
import edu.jhu.nlp.eval.PruningEfficiency;
import edu.jhu.nlp.eval.RelationEvaluator;
import edu.jhu.nlp.eval.SrlEvaluator;
import edu.jhu.nlp.eval.SrlEvaluator.SrlEvaluatorPrm;
import edu.jhu.nlp.eval.SrlPredIdAccuracy;
import edu.jhu.nlp.eval.SrlSelfLoops;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.features.TemplateReader;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.nlp.joint.IGFeatureTemplateSelector.IGFeatureTemplateSelectorPrm;
import edu.jhu.nlp.joint.JointNlpAnnotator.InitParams;
import edu.jhu.nlp.joint.JointNlpAnnotator.JointNlpAnnotatorPrm;
import edu.jhu.nlp.joint.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.nlp.relations.RelObsFe;
import edu.jhu.nlp.relations.RelObsFe.RelObsFePrm;
import edu.jhu.nlp.relations.RelationMunger;
import edu.jhu.nlp.relations.RelationMunger.RelationMungerPrm;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleStructure;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SrlFactorGraphBuilderPrm;
import edu.jhu.nlp.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.nlp.srl.SrlFeatureSelection;
import edu.jhu.nlp.tag.BrownClusterTagger;
import edu.jhu.nlp.tag.BrownClusterTagger.BrownClusterTaggerPrm;
import edu.jhu.nlp.tag.FileMapTagReducer;
import edu.jhu.nlp.words.PrefixAnnotator;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Prm;
import edu.jhu.util.Prng;
import edu.jhu.util.Threads;
import edu.jhu.util.Timer;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.files.Files;
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

    public static enum Optimizer { LBFGS, QN, SGD, ADAGRAD, ADAGRAD_COMID, ADADELTA, FOBOS, ASGD };

    public enum ErmaLoss { MSE, EXPECTED_RECALL, DP_DECODE_LOSS };

    public enum Inference { BRUTE_FORCE, BP, DP };
    
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

    private static final Logger log = LoggerFactory.getLogger(JointNlpRunner.class);
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
    @Opt(hasArg = true, description = "File to which to serialize the entire pipeline.")
    public static File pipeOut = null;
    
    // Options for initialization.
    @Opt(hasArg = true, description = "How to initialize the parameters of the model.")
    public static InitParams initParams = InitParams.UNIFORM;
    
    // Options for inference.
    @Opt(hasArg = true, description = "Type of inference method.")
    public static Inference inference = Inference.BP;
    @Opt(hasArg = true, description = "The algebra or semiring in which to run inference.")
    public static AlgebraType algebra = AlgebraType.LOG;
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
    @Opt(hasArg = true, description = "Max length for the brown clusters")
    public static int bcMaxTagLength = Integer.MAX_VALUE;
    
    // Options for Tag Maps
    @Opt(hasArg = true, description = "Type or file indicating tag mapping")
    public static File reduceTags = null;
    
    // Options for Embeddings.
    @Opt(hasArg=true, description="Path to word embeddings text file.")
    public static File embeddingsFile = null;
    @Opt(hasArg=true, description="Method for normalization of the embeddings.")
    public static Scaling embNorm = Scaling.L1_NORM;
    @Opt(hasArg=true, description="Amount to scale embeddings after normalization.")
    public static double embScalar = 15.0;
    
    // Options for SRL factor graph structure.
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
    @Opt(hasArg=true, description="Whether to do parameter averaging.")
    public static boolean sgdAveraging = false;
    @Opt(hasArg=true, description="Whether to do early stopping.")
    public static boolean sgdEarlyStopping = true;
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
    
    // Options for evaluation.
    @Opt(hasArg=true, description="Whether to skip punctuation in dependency parse evaluation.")
    public static boolean dpSkipPunctuation = false;
    @Opt(hasArg=true, description="Whether to evaluate test data.")
    public static boolean evalTest = true;
    
    private static ArgParser parser;
    
    public JointNlpRunner() { }

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
        
        AnnoPipeline anno = new AnnoPipeline();
        EvalPipeline eval = new EvalPipeline();
        JointNlpAnnotatorPrm prm = getJointNlpAnnotatorPrm();
        JointNlpAnnotator jointAnno = new JointNlpAnnotator(prm);
        if (modelIn != null) {
            jointAnno.loadModel(modelIn);
        }
        {
            RelationMunger relMunger = new RelationMunger(parser.getInstanceFromParsedArgs(RelationMungerPrm.class));
            if (CorpusHandler.getPredAts().contains(AT.REL_LABELS)) {
                anno.add(relMunger.getDataPreproc());
            }
            anno.add(new EnsureStaticOptionsAreSet());
            anno.add(new PrefixAnnotator(true));
            // Add Brown clusters.
            if (brownClusters != null) {
                anno.add(new BrownClusterTagger(getBrownCluterTaggerPrm(), brownClusters));
            } else {
                log.debug("No Brown clusters file specified.");
            }
            // Apply a tag map to reduce the POS tagset.
            if (reduceTags != null) {
                log.info("Reducing tags with file map: " + reduceTags);
                anno.add(new FileMapTagReducer(reduceTags));
            }
            // Add word embeddings.
            if (embeddingsFile != null) {
                anno.add(new EmbeddingsAnnotator(getEmbeddingsAnnotatorPrm()));
            } else {
                log.debug("No embeddings file specified.");
            }
            
            if (JointNlpRunner.modelIn == null) {
                // Feature selection at train time only for SRL.
                anno.add(new TransientAnnotator(new SrlFeatureSelection(prm.buPrm.fePrm)));
            }
            
            if (pruneByDist) {
                // Prune via the distance-based pruner.
                anno.add(new PosTagDistancePruner());
            }
            if (pruneByModel) {
                if (pruneModel == null) {
                    throw new IllegalStateException("If pruneByModel is true, pruneModel must be specified.");
                }
                anno.add(new FirstOrderPruner(pruneModel, getSrlFgExampleBuilderPrm(null), getDecoderPrm()));
            }
            if ((pruneByDist || pruneByModel ) && trainer == Trainer.CLL) {
                anno.add(new GoldDepParseUnpruner());
            }
            if (modelIn == null && prm.buPrm.fgPrm.srlPrm.predictPredPos) {
                // Predict SRL predicate positions as a separate step.
                JointNlpAnnotatorPrm prm2 = Prm.clone(prm);
                // Use the same features as the main jointAnno. These might be edited by feature selection.
                prm2.buPrm.fePrm = prm.buPrm.fePrm;
                // This is transient so we need to create another one.
                prm2.crfPrm = getCrfTrainerPrm();
                // Don't include anything except for SRL.
                prm2.buPrm.fgPrm.includeDp = false;
                prm2.buPrm.fgPrm.includeRel = false;
                SrlFactorGraphBuilderPrm srlPrm = prm2.buPrm.fgPrm.srlPrm;
                srlPrm.predictPredPos = true;
                srlPrm.predictSense = false;
                srlPrm.roleStructure = RoleStructure.NO_ROLES;
                anno.add(new JointNlpAnnotator(prm2));
                // Don't predict SRL predicate position in the main jointAnno below.
                prm.buPrm.fgPrm.srlPrm.predictPredPos = false;
                prm.buPrm.fgPrm.srlPrm.roleStructure = RoleStructure.PREDS_GIVEN;
            }
            // Various NLP annotations.
            anno.add(jointAnno);
            if (CorpusHandler.getPredAts().contains(AT.REL_LABELS) && !relMunger.getPrm().makeRelSingletons) {
                anno.add(relMunger.getDataPostproc());
            }
        }
        {
            if (pruneByDist || pruneByModel) {
                eval.add(new PruningEfficiency(dpSkipPunctuation));
                eval.add(new OraclePruningAccuracy(dpSkipPunctuation));
                eval.add(new OraclePruningExactMatch(dpSkipPunctuation));
            }
            if (CorpusHandler.getGoldOnlyAts().contains(AT.DEP_TREE)) {
                eval.add(new DepParseAccuracy(dpSkipPunctuation));
                eval.add(new DepParseExactMatch(dpSkipPunctuation));
            }
            if (CorpusHandler.getGoldOnlyAts().contains(AT.SRL_PRED_IDX)) {
                // Evaluate F1 of unlabled predicate position identification.
                eval.add(new SrlEvaluator(new SrlEvaluatorPrm(false, false, predictPredPos, false)));
                eval.add(new SrlPredIdAccuracy());
            }
            if (CorpusHandler.getGoldOnlyAts().contains(AT.SRL)) {
                eval.add(new SrlSelfLoops());
                eval.add(new SrlEvaluator(new SrlEvaluatorPrm(true, predictSense, predictPredPos, (roleStructure != RoleStructure.NO_ROLES))));
            }
            if (CorpusHandler.getGoldOnlyAts().contains(AT.REL_LABELS)) {
                eval.add(new RelationEvaluator());
            }
            eval.add(new ProportionAnnotated(CorpusHandler.getPredAts()));
        }

        AnnoSentenceCollection devGold = null;
        AnnoSentenceCollection devInput = null;
        if (corpus.hasTrain()) {
            String name = "train";            
            AnnoSentenceCollection trainGold = corpus.getTrainGold();
            AnnoSentenceCollection trainInput = corpus.getTrainInput();
            // (Dev data might be null.)
            devGold = corpus.getDevGold();
            devInput = corpus.getDevInput();
            
            // Train a model. (The PipelineAnnotator also annotates all the input.)
            anno.train(trainInput, trainGold, devInput, devGold);
            
            // Decode and evaluate the train data.
            corpus.writeTrainPreds(trainInput);
            eval.evaluate(trainInput, trainGold, name);
            corpus.clearTrainCache();
            
            if (modelOut != null) {
                jointAnno.saveModel(modelOut);
            }
            if (printModel != null) {
                jointAnno.printModel(printModel);
            }
            if (pipeOut != null) {
                log.info("Serializing pipeline to file: " + pipeOut);
                Files.serialize(anno, pipeOut);
            }
        }
        
        if (corpus.hasDev()) {
            // Write dev data predictions.
            String name = "dev";
            if (devInput == null) {
                // Train did not yet annotate the dev data.
                devInput = corpus.getDevInput();
                anno.annotate(devInput);
            }
            corpus.writeDevPreds(devInput);
            // Evaluate dev data.
            devGold = corpus.getDevGold();
            eval.evaluate(devInput, devGold, name);
            corpus.clearDevCache();
        }
        
        if (corpus.hasTest()) {
            // Decode test data.
            String name = "test";
            AnnoSentenceCollection testInput = corpus.getTestInput();
            anno.annotate(testInput);
            corpus.writeTestPreds(testInput);
            // Evaluate test data.
            AnnoSentenceCollection testGold = corpus.getTestGold();
            if (evalTest) {
                eval.evaluate(testInput, testGold, name);
            } else {
                (new ProportionAnnotated(CorpusHandler.getPredAts())).evaluate(testInput, testGold, name);
            }
            corpus.clearTestCache();
        }
        t.stop();
        rep.report("elapsedSec", t.totSec());
    }
    
    /**
     * TODO: Deprecate this class. This is only a hold over until we remove the dependence of
     * CommunicationsAnnotator on these options being correctly set.
     * 
     * @author mgormley
     */
    public static class EnsureStaticOptionsAreSet implements Annotator {        
        private static final long serialVersionUID = 1L;
        private static final Logger log = LoggerFactory.getLogger(EnsureStaticOptionsAreSet.class);
        private final boolean singleRoot = InsideOutsideDepParse.singleRoot;
        private final boolean useLogAddTable = JointNlpRunner.useLogAddTable;
        @Override
        public void annotate(AnnoSentenceCollection sents) {
            log.info("Ensuring that static options (singleRoot and useLogAddTable) are set to their train-time values.");
            InsideOutsideDepParse.singleRoot = singleRoot;
            FastMath.useLogAddTable = useLogAddTable;
        }
        @Override
        public Set<AT> getAnnoTypes() {
            return Collections.emptySet();
        }
    }
    
    /* --------- Factory Methods ---------- */

    public static IGFeatureTemplateSelectorPrm getInformationGainFeatureSelectorPrm() {
        IGFeatureTemplateSelectorPrm prm = new IGFeatureTemplateSelectorPrm();
        prm.featureHashMod = featureHashMod;
        prm.numThreads = threads;
        prm.numToSelect = numFeatsToSelect;
        prm.maxNumSentences = numSentsForFeatSelect;
        prm.selectSense = predictSense;
        return prm;
    }

    private static JointNlpAnnotatorPrm getJointNlpAnnotatorPrm() throws ParseException {
        JointNlpAnnotatorPrm prm = new JointNlpAnnotatorPrm();
        prm.crfPrm = getCrfTrainerPrm();
        prm.csPrm = getCorpusStatisticsPrm();
        prm.dePrm = getDecoderPrm();
        prm.initParams = initParams;
        prm.ofcPrm = getObsFeatureConjoinerPrm();
        prm.dpSkipPunctuation = dpSkipPunctuation;
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
        if (CorpusHandler.getGoldOnlyAts().contains(AT.REL_LABELS)) {
            prm.fgPrm.relPrm.fePrm = parser.getInstanceFromParsedArgs(RelObsFePrm.class);
        }
        
        prm.fgPrm.includeDp = CorpusHandler.getGoldOnlyAts().contains(AT.DEP_TREE);
        prm.fgPrm.includeSrl = CorpusHandler.getGoldOnlyAts().contains(AT.SRL);
        prm.fgPrm.includeRel = CorpusHandler.getGoldOnlyAts().contains(AT.REL_LABELS);
        
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
        if (CorpusHandler.getGoldOnlyAts().contains(AT.SRL) && acl14DepFeats) {
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

    public static CorpusStatisticsPrm getCorpusStatisticsPrm() {
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
        } else if (optimizer == Optimizer.SGD || optimizer == Optimizer.ASGD  ||
                optimizer == Optimizer.ADAGRAD || optimizer == Optimizer.ADADELTA) {
            prm.optimizer = null;
            SGDPrm sgdPrm = getSgdPrm();
            if (optimizer == Optimizer.SGD){
                BottouSchedulePrm boPrm = new BottouSchedulePrm();
                boPrm.initialLr = sgdInitialLr;
                boPrm.lambda = 1.0 / l2variance;
                sgdPrm.sched = new BottouSchedule(boPrm);
            } else if (optimizer == Optimizer.ASGD){
                BottouSchedulePrm boPrm = new BottouSchedulePrm();
                boPrm.initialLr = sgdInitialLr;
                boPrm.lambda = 1.0 / l2variance;
                boPrm.power = 0.75;
                sgdPrm.sched = new BottouSchedule(boPrm);
                sgdPrm.averaging = true;
            } else if (optimizer == Optimizer.ADAGRAD){
                AdaGradSchedulePrm adaGradPrm = new AdaGradSchedulePrm();
                adaGradPrm.eta = adaGradEta;
                adaGradPrm.constantAddend = adaDeltaConstantAddend;
                sgdPrm.sched = new AdaGradSchedule(adaGradPrm);
            } else if (optimizer == Optimizer.ADADELTA){
                AdaDeltaPrm adaDeltaPrm = new AdaDeltaPrm();
                adaDeltaPrm.decayRate = adaDeltaDecayRate;
                adaDeltaPrm.constantAddend = adaDeltaConstantAddend;
                sgdPrm.sched = new AdaDelta(adaDeltaPrm);
                sgdPrm.autoSelectLr = false;
            }
            prm.batchOptimizer = new SGD(sgdPrm);
        } else if (optimizer == Optimizer.ADAGRAD_COMID) {
            AdaGradComidL2Prm sgdPrm = new AdaGradComidL2Prm();
            setSgdPrm(sgdPrm);
            //TODO: sgdPrm.l1Lambda = l2Lambda;
            sgdPrm.l2Lambda = 1.0 / l2variance;
            sgdPrm.eta = adaGradEta;
            sgdPrm.constantAddend = adaDeltaConstantAddend;
            sgdPrm.sched = null;
            prm.optimizer = null;
            prm.batchOptimizer = new AdaGradComidL2(sgdPrm);
        } else if (optimizer == Optimizer.FOBOS) {
            SGDFobosPrm sgdPrm = new SGDFobosPrm();
            setSgdPrm(sgdPrm);
            //TODO: sgdPrm.l1Lambda = l2Lambda;            
            sgdPrm.l2Lambda = 1.0 / l2variance;
            BottouSchedulePrm boPrm = new BottouSchedulePrm();
            boPrm.initialLr = sgdInitialLr;
            boPrm.lambda = 1.0 / l2variance;
            sgdPrm.sched = new BottouSchedule(boPrm);  
            prm.optimizer = null;
            prm.batchOptimizer = new SGDFobos(sgdPrm);
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
        if (prm.trainer == Trainer.ERMA && 
                CorpusHandler.getPredAts().equals(Lists.getList(AT.DEP_TREE))) {
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
        setSgdPrm(prm);
        return prm;
    }

    private static void setSgdPrm(SGDPrm prm) {
        prm.numPasses = sgdNumPasses;
        prm.batchSize = sgdBatchSize;
        prm.withReplacement = sgdWithRepl;
        prm.stopBy = stopTrainingBy;
        prm.autoSelectLr = sgdAutoSelectLr;
        prm.autoSelectFreq = sgdAutoSelecFreq;
        prm.computeValueOnNonFinalIter = sgdComputeValueOnNonFinalIter;
        prm.averaging = sgdAveraging; 
        prm.earlyStopping = sgdEarlyStopping; 
        // Make sure we correctly set the schedule somewhere else.
        prm.sched = null;
    }

    private static FgInferencerFactory getInfFactory() throws ParseException {
        if (inference == Inference.BRUTE_FORCE) {
            BruteForceInferencerPrm prm = new BruteForceInferencerPrm(algebra.getAlgebra());
            return prm;
        } else if (inference == Inference.BP) {
            ErmaBpPrm bpPrm = new ErmaBpPrm();
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
        } else if (inference == Inference.DP) {
            if (CorpusHandler.getPredAts().size() == 1 && CorpusHandler.getPredAts().get(0) == AT.DEP_TREE
                    && grandparentFactors && !siblingFactors) { 
                return new O2AllGraFgInferencerFactory(algebra.getAlgebra());
            } else {
                throw new ParseException("DP inference only supported for dependency parsing with all grandparent factors.");
            }
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
        bcPrm.maxTagLength = bcMaxTagLength;
        return bcPrm;
    }

    private static EmbeddingsAnnotatorPrm getEmbeddingsAnnotatorPrm() {
        EmbeddingsAnnotatorPrm prm = new EmbeddingsAnnotatorPrm();
        prm.embeddingsFile = embeddingsFile;
        prm.embNorm = embNorm;
        prm.embScalar= embScalar;
        return prm;
    }
    
    public static void main(String[] args) {
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(JointNlpRunner.class);
            parser.registerClass(JointNlpRunner.class);
            parser.registerClass(CorpusHandler.class);
            parser.registerClass(RelationMungerPrm.class);
            parser.registerClass(RelObsFePrm.class);
            parser.registerClass(InsideOutsideDepParse.class);      
            parser.registerClass(ReporterManager.class);
            parser.parseArgs(args);
            JointNlpRunner.parser = parser;
            
            ReporterManager.init(ReporterManager.reportOut, true);
            Prng.seed(seed);
            Threads.initDefaultPool(threads);

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
            Threads.shutdownDefaultPool();
            ReporterManager.close();
        }
        
        System.exit(exitCode);
    }

}
