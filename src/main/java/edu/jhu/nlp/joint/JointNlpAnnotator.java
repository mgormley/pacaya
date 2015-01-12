package edu.jhu.nlp.joint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.Evaluator;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.eval.DepParseAccuracy;
import edu.jhu.nlp.eval.RelationEvaluator;
import edu.jhu.nlp.eval.SrlEvaluator;
import edu.jhu.nlp.eval.SrlEvaluator.SrlEvaluatorPrm;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.joint.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleStructure;
import edu.jhu.prim.util.Lambda.FnIntToVoid;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Prm;
import edu.jhu.util.Threads;
import edu.jhu.util.Timer;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.files.Files;

/**
 * Joint annotator for SRL and syntactic dependency parsing.
 * 
 * @author mgormley
 */
public class JointNlpAnnotator implements Trainable, Annotator {

    public static enum InitParams { UNIFORM, RANDOM };
    private static final long serialVersionUID = 1L;

    public static class JointNlpAnnotatorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        
        public JointNlpFgExampleBuilderPrm buPrm = new JointNlpFgExampleBuilderPrm();
        public JointNlpDecoderPrm dePrm = new JointNlpDecoderPrm();
        // How to initialize the parameters of the model
        public InitParams initParams = InitParams.UNIFORM;
        // Whether to skip punctuation when evaluating dependency parsing.
        public boolean dpSkipPunctuation = false;
        // --------------------------------------------------------------------
        // These parameters are only used if a NEW model is created. If a model
        // is loaded from disk, these are ignored.
        public transient CrfTrainerPrm crfPrm = new CrfTrainerPrm();
        public CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm(); 
        public ObsFeatureConjoinerPrm ofcPrm = new ObsFeatureConjoinerPrm();
        // We also ignore buPrm.fePrm, which is overwritten by the value in the model.
        // --------------------------------------------------------------------
    }
    
    private static final Logger log = LoggerFactory.getLogger(JointNlpAnnotator.class);
    private JointNlpAnnotatorPrm prm;   
    private JointNlpFgModel model = null;

    public JointNlpAnnotator(JointNlpAnnotatorPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold, 
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        log.info("Initializing data.");
        CorpusStatistics cs;
        ObsFeatureConjoiner ofc;
        if (model == null) {
            FactorTemplateList fts = new FactorTemplateList();
            ofc = new ObsFeatureConjoiner(prm.ofcPrm, fts);
            cs = new CorpusStatistics(prm.csPrm);
        } else {
            ofc = model.getOfc();
            cs = model.getCs();
            ofc.getTemplates().startGrowth();
        }
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm.buPrm, ofc, cs, true);
        FgExampleList data = builder.getData(trainInput, trainGold);
        
        if (model == null) {
            model = new JointNlpFgModel(cs, ofc, prm.buPrm.fePrm);
            if (prm.initParams == InitParams.RANDOM) {
                log.info("Initializing model with zero-one Gaussian.");
                model.setRandomStandardNormal();
            } else if (prm.initParams == InitParams.UNIFORM) {
                log.info("Initializing model to all zeros.");
                // Do nothing.
            } else {
                throw new RuntimeException("Parameter initialization method not implemented: " + prm.initParams);
            }
        } else {
            log.info("Using read model as initial parameters for training.");
        }
        log.info(String.format("Num model params: %d", model.getNumParams()));
        
        log.info("Training model.");
        CrfTrainer trainer = new CrfTrainer(prm.crfPrm);
        trainer.train(model, data, getValidationFn(devInput, devGold));
        ofc.getTemplates().stopGrowth();
    }
    
    private Function getValidationFn(final AnnoSentenceCollection devInput, final AnnoSentenceCollection devGold) {
        if (devInput == null || devGold == null) { return null; }
        final JointNlpAnnotator anno = this;
        final Evaluator eval;
        if (CorpusHandler.getPredAts().equals(Lists.getList(AT.DEP_TREE))) {
            eval = new DepParseAccuracy(prm.dpSkipPunctuation);
        } else if (CorpusHandler.getPredAts().equals(Lists.getList(AT.SRL)) || 
                CorpusHandler.getPredAts().equals(Lists.getList(AT.SRL_PRED_IDX, AT.SRL)) ||
                CorpusHandler.getPredAts().equals(Lists.getList(AT.SRL, AT.SRL_PRED_IDX))
                ) {
            SrlEvaluatorPrm evalPrm = new SrlEvaluatorPrm();
            evalPrm.evalSense = prm.buPrm.fgPrm.srlPrm.predictSense;
            evalPrm.evalPredicatePosition = prm.buPrm.fgPrm.srlPrm.predictPredPos;
            evalPrm.evalRoles = (prm.buPrm.fgPrm.srlPrm.roleStructure != RoleStructure.NO_ROLES);
            eval = new SrlEvaluator(evalPrm);
        } else if (CorpusHandler.getPredAts().equals(Lists.getList(AT.REL_LABELS))) {
            eval = new RelationEvaluator();
        } else {
            log.warn("Validation function not implemented. Skipping.");
            return null;
        }

        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm.buPrm, model.getOfc(), model.getCs(), false);
        final FgExampleList devData = builder.getData(devInput, null);
        return new Function() {
            
            @Override
            public double getValue(IntDoubleVector point) {
                // TODO: This should make a shallow copy of the input sentences.
                anno.annotate(devInput, devData);
                return eval.evaluate(devInput, devGold, "dev");
            }
            
            @Override
            public int getNumDimensions() {
                return -1;
            }
        };
    }

    @Override
    public void annotate(AnnoSentenceCollection sents) {
        if (model == null) {
            throw new IllegalStateException("No model exists. Must call train() or loadModel() before annotate().");
        }
        
        log.info("Running the decoder");
        Timer timer = new Timer();
        timer.start();
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm.buPrm, model.getOfc(), model.getCs(), false);
        FgExampleList data = builder.getData(sents, null);  
        annotate(sents, data);
        timer.stop();
        log.info(String.format("Decoded at %.2f tokens/sec with %d threads", sents.getNumTokens() / timer.totSec(), Threads.numThreads));
    }

    private void annotate(final AnnoSentenceCollection sents, final FgExampleList data) {
        // Add the new predictions to the input sentences.
        Threads.forEach(0, sents.size(), new FnIntToVoid() {            
            @Override
            public void call(int i) {
                try {
                    UFgExample ex = data.get(i);
                    AnnoSentence inputSent = sents.get(i);
                    JointNlpDecoder decoder = new JointNlpDecoder(prm.dePrm);
                    AnnoSentence predSent = decoder.decode(model, ex, inputSent);
                    sents.set(i, predSent);
                } catch (Throwable t) {
                    AbstractParallelAnnotator.logThrowable(log, t);
                }
            }
        });
    }
    
    public void loadModel(File modelIn) {
        // Read a model from a file.
        log.info("Reading model from file: " + modelIn);
        loadModel((JointNlpFgModel) Files.deserialize(modelIn));
    }
    
    public void loadModel(JointNlpFgModel model) {
        this.model = model;
        // Restore the feature extractor parameters from the serialized model.
        prm.buPrm.fePrm = model.getFePrm();
    }

    public void saveModel(File modelOut) {
        // Write the model to a file.
        log.info("Serializing model to file: " + modelOut);
        Files.serialize(model, modelOut);
    }

    public void printModel(File printModel) throws IOException {
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

}
