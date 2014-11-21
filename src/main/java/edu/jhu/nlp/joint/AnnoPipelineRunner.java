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
import edu.jhu.nlp.InformationGainFeatureTemplateSelector;
import edu.jhu.nlp.InformationGainFeatureTemplateSelector.InformationGainFeatureTemplateSelectorPrm;
import edu.jhu.nlp.InformationGainFeatureTemplateSelector.SrlFeatTemplates;
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
import edu.jhu.nlp.tag.FileMapTagReducer;
import edu.jhu.nlp.words.PrefixAnnotator;
import edu.jhu.prim.util.math.FastMath;
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
public class AnnoPipelineRunner {

    private static final Logger log = LoggerFactory.getLogger(AnnoPipelineRunner.class);
    private static final Reporter rep = Reporter.getReporter(AnnoPipelineRunner.class);

    // Options not specific to the model
    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    @Opt(hasArg = true, description = "Whether to use a log-add table for faster computation.")
    public static boolean useLogAddTable = false;
    
    // Options for model IO
    @Opt(hasArg = true, description = "File from which to read a serialized pipeline.")
    public static File pipeIn = null;
        
    public AnnoPipelineRunner() {
    }

    public void run() throws ParseException, IOException {  
        Timer t = new Timer();
        t.start();
        // Initialize the data reader/writer.
        CorpusHandler corpus = new CorpusHandler();

        if (pipeIn == null) {
            throw new ParseException("pipeIn must not be null");
        }
        AnnoPipeline anno = (AnnoPipeline) Files.deserialize(pipeIn);
        if (corpus.hasTest()) {
            // Decode test data.
            AnnoSentenceCollection testInput = corpus.getTestInput();
            anno.annotate(testInput);
            (new ProportionAnnotated(CorpusHandler.getPredAts())).evaluate(testInput, null, "test");
            corpus.writeTestPreds(testInput);
            corpus.clearTestCache();
        }
        t.stop();
        rep.report("elapsedSec", t.totSec());
    }

    public static void main(String[] args) {
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(AnnoPipelineRunner.class);
            parser.addClass(AnnoPipelineRunner.class);
            parser.addClass(CorpusHandler.class);
            // TODO: What to do about these? parser.addClass(RelationsOptions.class);
            parser.addClass(ReporterManager.class);
            parser.parseArgs(args);
            
            ReporterManager.init(ReporterManager.reportOut, true);
            Prng.seed(seed);
            Threads.initDefaultPool(threads);

            AnnoPipelineRunner pipeline = new AnnoPipelineRunner();
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
