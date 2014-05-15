package edu.jhu.srl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.train.CrfTrainer;
import edu.jhu.gm.train.CrfTrainer.CrfTrainerPrm;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.srl.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.util.Prm;
import edu.jhu.util.Timer;
import edu.jhu.util.files.Files;

/**
 * Joint annotator for SRL and syntactic dependency parsing.
 * 
 * @author mgormley
 */
public class JointNlpAnnotator implements Trainable, Annotator {

    public static enum InitParams { UNIFORM, RANDOM };
    
    public static class JointNlpAnnotatorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        
        public JointNlpFgExampleBuilderPrm buPrm = new JointNlpFgExampleBuilderPrm();
        public CrfTrainerPrm crfPrm = new CrfTrainerPrm();
        public JointNlpDecoderPrm dePrm = new JointNlpDecoderPrm();
        // How to initialize the parameters of the model
        public InitParams initParams = InitParams.UNIFORM;

        // --------------------------------------------------------------------
        // These parameters are only used if a NEW model is created. If a model
        // is loaded from disk, these are ignored.
        public CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm(); 
        public ObsFeatureConjoinerPrm ofcPrm = new ObsFeatureConjoinerPrm();
        // We also ignore buPrm.fePrm, which is overwritten by the value in the model.
        // --------------------------------------------------------------------
    }
    
    private static final Logger log = Logger.getLogger(JointNlpAnnotator.class);
    private JointNlpAnnotatorPrm prm;   
    private JointNlpFgModel model = null;

    public JointNlpAnnotator(JointNlpAnnotatorPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public void train(SimpleAnnoSentenceCollection sents) {
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
        FgExampleList data = builder.getData(sents);
        

        log.info("Initializing model.");        
        if (model == null) {
            model = new JointNlpFgModel(cs, ofc, prm.buPrm.fePrm);
            if (prm.initParams == InitParams.RANDOM) {
                model.setRandomStandardNormal();
            } else if (prm.initParams == InitParams.UNIFORM) {
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
        trainer.train(model, data);
        ofc.getTemplates().stopGrowth();
    }
    
    @Override
    public void annotate(SimpleAnnoSentenceCollection sents) {
        if (model == null) {
            throw new IllegalStateException("No model exists. Must call train() or loadModel() before annotate().");
        }
        
        log.info("Running the decoder");
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(prm.buPrm, model.getOfc(), model.getCs(), false);
        FgExampleList data = builder.getData(sents);
        
        Timer timer = new Timer();
        timer.start();
        // Add the new predictions to the input sentences.
        for (int i = 0; i < sents.size(); i++) {
            UFgExample ex = data.get(i);
            SimpleAnnoSentence predSent = sents.get(i);
            JointNlpDecoder decoder = new JointNlpDecoder(prm.dePrm);
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
        log.info(String.format("Decoded at %.2f tokens/sec", sents.getNumTokens() / timer.totSec()));        
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
