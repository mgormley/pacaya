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
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateReader;
import edu.jhu.featurize.TemplateSets;
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
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.InformationGainFeatureTemplateSelectorPrm;
import edu.jhu.srl.InformationGainFeatureTemplateSelector.SrlFeatTemplates;
import edu.jhu.srl.SrlDecoder.SrlDecoderPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.tag.BrownClusterTagger;
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

    public static enum DatasetType { ERMA, CONLL_2009 };

    public static enum InitParams { UNIFORM, RANDOM };
    
    public static enum Optimizer { LBFGS, SGD, ADAGRAD, ADADELTA };
    
    private static final Logger log = Logger.getLogger(SrlRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    
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

    // Options for train/test data
    @Opt(hasArg = true, description = "Brown cluster file")
    public static File brownClusters = null;    
    
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
    @Opt(hasArg = true, description = "Whether to do feature selection.")
    public static boolean featureSelection = true;
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
    @Opt(hasArg = true, description = "Whether to remove the lemma and plemma columns from CoNLL-2009 data.")
    public static boolean removeLemma = false;
    @Opt(hasArg = true, description = "Whether to remove the feat and pfeat columns from CoNLL-2009 data.")
    public static boolean removeFeat = false;

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

    public SrlRunner() {
    }

    public void run() throws ParseException, IOException {  
        if (logDomain) {
            FastMath.useLogAddTable = true;
        }
        
        // Get a model.
        SrlFgModel model = null;
        FactorTemplateList fts;
        CorpusStatistics cs;
        SrlFeatureExtractorPrm srlFePrm; // TODO: USE THIS!!!!
        if (modelIn != null) {
            // Read a model from a file.
            log.info("Reading model from file: " + modelIn);
            model = (SrlFgModel) Files.deserialize(modelIn);
            fts = model.getTemplates();
            cs = model.getCs();
            srlFePrm = model.getSrlFePrm();
        } else {
            srlFePrm = getSrlFeatureExtractorPrm();
            cs = new CorpusStatistics(getCorpusStatisticsPrm());
            if (useTemplates && featureSelection) {
                String name = "train";
                SimpleAnnoSentenceCollection sents = readSentences(cs.prm.useGoldSyntax, trainType, train,
                        trainGoldOut, trainMaxNumSentences, trainMaxSentenceLength, name);
                CorpusStatisticsPrm csPrm = getCorpusStatisticsPrm();
                
                InformationGainFeatureTemplateSelectorPrm prm = new InformationGainFeatureTemplateSelectorPrm();
                prm.featureHashMod = featureHashMod;
                prm.numThreads = threads;
                prm.numToSelect = 32;
                InformationGainFeatureTemplateSelector ig = new InformationGainFeatureTemplateSelector(prm);
                SrlFeatTemplates sft = ig.getFeatTemplatesForSrl(sents, csPrm);
                ig.shutdown();
                srlFePrm.fePrm.soloTemplates = sft.srlSenseTemplates;
                srlFePrm.fePrm.pairTemplates = sft.srlArgTemplates;                
            }
            if (useTemplates) {
                log.info("Num sense feature templates: " + srlFePrm.fePrm.soloTemplates.size());
                log.info("Num arg feature templates: " + srlFePrm.fePrm.pairTemplates.size());
            }
            fts = new FactorTemplateList();
        }
        
        if (trainType != null && train != null) {
            String name = "train";
            // Train a model.
            FgExampleList data = getData(fts, cs, trainType, train, trainGoldOut, trainMaxNumSentences,
                    trainMaxSentenceLength, name, srlFePrm);
            
            if (model == null) {
                model = new SrlFgModel(data, includeUnsupportedFeatures, cs, srlFePrm);
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
            VarConfigPair pair = decode(model, data, trainType, trainPredOut, name);        
            eval(name, pair);
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

        if (test != null && testType != null) {
            // Test the model on test data.
            fts.stopGrowth();
            String name = "test";
            FgExampleList data = getData(fts, cs, testType, test, testGoldOut, testMaxNumSentences,
                    testMaxSentenceLength, name, srlFePrm);
            // Decode and evaluate the test data.
            VarConfigPair pair = decode(model, data, testType, testPredOut, name);
            eval(name, pair);
        }
    }

    private FgExampleList getData(FactorTemplateList fts, CorpusStatistics cs, DatasetType dataType, File dataFile, File goldFile,
            int maxNumSentences, int maxSentenceLength, String name, SrlFeatureExtractorPrm srlFePrm) throws ParseException, IOException {
        SimpleAnnoSentenceCollection sents = readSentences(cs.prm.useGoldSyntax, dataType, dataFile, goldFile, maxNumSentences,
                maxSentenceLength, name);        
        return getData(fts, cs, name, sents, srlFePrm);
    }

    private FgExampleList getData(FactorTemplateList fts, CorpusStatistics cs, String name,
            SimpleAnnoSentenceCollection sents, SrlFeatureExtractorPrm srlFePrm) {
        if (!cs.isInitialized()) {
            log.info("Initializing corpus statistics.");
            cs.init(sents);
        }

        if (useTemplates) {
            SimpleAnnoSentence sent = sents.get(0);
            TemplateLanguage.assertRequiredAnnotationTypes(sent, srlFePrm.fePrm.soloTemplates);
            TemplateLanguage.assertRequiredAnnotationTypes(sent, srlFePrm.fePrm.pairTemplates);
        }
        
        log.info("Building factor graphs and extracting features.");
        SrlFgExampleBuilderPrm prm = getSrlFgExampleBuilderPrm(srlFePrm);        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, fts, cs);
        FgExampleList data = builder.getData(sents);
        
        // Special case: we somehow need to be able to create test examples
        // where we've never seen the predicate.
        if (prm.fgPrm.predictSense) {
            fts.startGrowth();
            // TODO: This should have a bias feature.
            Var v = new Var(VarType.PREDICTED, 1, CorpusStatistics.UNKNOWN_SENSE, CorpusStatistics.SENSES_FOR_UNK_PRED);
            fts.add(new FactorTemplate(new VarSet(v), new Alphabet<Feature>(), SrlFactorGraph.TEMPLATE_KEY_FOR_UNKNOWN_SENSE));
            fts.stopGrowth();
        }
        
        log.info(String.format("Num examples in %s: %d", name, data.size()));
        // TODO: log.info(String.format("Num factors in %s: %d", name, data.getNumFactors()));
        // TODO: log.info(String.format("Num variables in %s: %d", name, data.getNumVars()));
        log.info(String.format("Num factor/clique templates: %d", data.getTemplates().size()));
        log.info(String.format("Num observation function features: %d", data.getTemplates().getNumObsFeats()));
        return data;
    }

    private SimpleAnnoSentenceCollection readSentences(boolean useGoldSyntax, DatasetType dataType, File dataFile,
            File goldFile, int maxNumSentences, int maxSentenceLength, String name) throws IOException, ParseException {
        log.info("Reading " + name + " data of type " + dataType + " from " + dataFile);
        SimpleAnnoSentenceCollection sents;
        int numTokens = 0;
        
        // Read the data and (optionally) write it to the gold file.
        if (dataType == DatasetType.CONLL_2009) {
            List<CoNLL09Sentence> conllSents = new ArrayList<CoNLL09Sentence>();
            CoNLL09FileReader reader = new CoNLL09FileReader(dataFile);
            for (CoNLL09Sentence sent : reader) {
                if (conllSents.size() >= maxNumSentences) {
                    break;
                }
                if (sent.size() <= maxSentenceLength) {
                    sent.intern();
                    conllSents.add(sent);
                    numTokens += sent.size();
                }
            }
            reader.close();     

            if (normalizeRoleNames) {
                log.info("Normalizing role names");
                for (CoNLL09Sentence conllSent : conllSents) {
                    conllSent.normalizeRoleNames();
                }
            }
            
            if (goldFile != null) {
                log.info("Writing gold data to file: " + goldFile);
                CoNLL09Writer cw = new CoNLL09Writer(goldFile);
                for (CoNLL09Sentence sent : conllSents) {
                    cw.write(sent);
                }
                cw.close();
            }

            // Data munging -- this must be done after we write the gold sentences to a file.
            reduceSupervision(conllSents);
                        
            // TODO: We should clearly differentiate between the gold sentences and the input sentence.
            // Convert CoNLL sentences to SimpleAnnoSentences.
            sents = new SimpleAnnoSentenceCollection();
            for (CoNLL09Sentence conllSent : conllSents) {
                sents.add(conllSent.toSimpleAnnoSentence(useGoldSyntax));
            }
        } else {
            throw new ParseException("Unsupported data type: " + dataType);
        }
        
        log.info("Num " + name + " sentences: " + sents.size());   
        log.info("Num " + name + " tokens: " + numTokens);

        if (brownClusters != null) {
            log.info("Adding Brown clusters.");
            BrownClusterTagger bct = new BrownClusterTagger(Integer.MAX_VALUE);
            bct.read(brownClusters);
            bct.addClusters(sents);
            log.info("Brown cluster miss rate: " + bct.getMissRate());
        } else {
            log.warn("No Brown cluster file specified.");            
        }
        return sents;
    }

    /** Remove various aspects of supervision from the data. */
    private void reduceSupervision(List<CoNLL09Sentence> conllSents) {
        if (useProjDepTreeFactor) {
            // TODO: This should be a removeHead option, which is usually
            // set to true whenever we have latent syntax.
            log.info("Removing all dependency trees from the CoNLL data");
            for (CoNLL09Sentence conllSent : conllSents) {
                conllSent.removeHeadAndPhead();
                conllSent.removeDeprealAndPdeprel();
            }
        } else if (removeDeprel) {
            log.info("Removing syntactic dependency labels from the CoNLL data");  
            for (CoNLL09Sentence conllSent : conllSents) {
                conllSent.removeDeprealAndPdeprel();
            }
        } 
        
        if (removeLemma) {
            log.info("Removing lemmas from the CoNLL data");  
            for (CoNLL09Sentence conllSent : conllSents) {
                conllSent.removeLemmaAndPlemma();
            }
        }
        
        if (removeFeat) {
            log.info("Removing morphological features from the CoNLL data");  
            for (CoNLL09Sentence conllSent : conllSents) {
                conllSent.removeFeatAndPfeat();
            }
        }
    }
    
    private void eval(String name, VarConfigPair pair) {
        AccuracyEvaluator accEval = new AccuracyEvaluator();
        double accuracy = accEval.evaluate(pair.gold, pair.pred);
        log.info(String.format("Accuracy on %s: %.6f", name, accuracy));
    }
    
    private VarConfigPair decode(FgModel model, FgExampleList data, DatasetType dataType, File predOut, String name) throws IOException, ParseException {
        log.info("Running the decoder on " + name + " data.");
        
        SimpleAnnoSentenceCollection goldSents = (SimpleAnnoSentenceCollection) data.getSourceSentences();
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
        
        if (predOut != null) {
            log.info("Writing predictions for " + name + " data of type " + dataType + " to " + predOut);
            if (dataType == DatasetType.CONLL_2009) {
                CoNLL09Writer cw = new CoNLL09Writer(predOut);
                for (SimpleAnnoSentence sent : predSents) {
                    CoNLL09Sentence conllSent = CoNLL09Sentence.fromSimpleAnnoSentence(sent);
                    cw.write(conllSent);
                }
                cw.close();
            } else {
                throw new ParseException("Unsupported data type: " + dataType);
            }
        }
        
        return new VarConfigPair(goldVcs, predVcs);
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
        prm.exPrm.featCountCutoff = featCountCutoff;
        prm.exPrm.cacheType = cacheType;
        prm.exPrm.gzipped = gzipCache;
        prm.exPrm.maxEntriesInMemory = maxEntriesInMemory;
        
        // SRL Feature Extraction.
        prm.srlFePrm.featureHashMod = featureHashMod;
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
        Collection<FeatTemplate> tpls = new HashSet<FeatTemplate>();

        TemplateReader tr = new TemplateReader();
        for (String path : featTpls.split(":")) {
            if (path.equals("coarse")) {
                List<FeatTemplate> coarseUnigramSet1 = TemplateSets.getCoarseUnigramSet1();
                if (brownClusters == null) {
                    // Filter out the Brown cluster features.
                    log.warn("Filtering out Brown cluster features from coarse set.");
                    coarseUnigramSet1 = TemplateLanguage.filterOutRequiring(coarseUnigramSet1, AT.BROWN);
                }
                tpls.addAll(coarseUnigramSet1);
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
        prm.crfObjPrm.numThreads = threads;
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
