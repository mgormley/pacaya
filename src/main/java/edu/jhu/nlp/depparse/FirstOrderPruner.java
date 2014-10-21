package edu.jhu.nlp.depparse;

import java.io.File;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.joint.JointNlpDecoder;
import edu.jhu.nlp.joint.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.nlp.joint.JointNlpFgModel;
import edu.jhu.util.Prm;
import edu.jhu.util.Timer;
import edu.jhu.util.files.Files;

public class FirstOrderPruner implements Annotator {

    private static final Logger log = Logger.getLogger(FirstOrderPruner.class);
    private File pruneModel;
    private JointNlpFgExampleBuilderPrm exPrm;
    private JointNlpDecoderPrm dPrm;

    public FirstOrderPruner(File pruneModel, JointNlpFgExampleBuilderPrm exPrm, JointNlpDecoderPrm dPrm) {
        this.pruneModel = pruneModel;
        this.exPrm = Prm.clonePrm(exPrm);
        this.dPrm = dPrm;
    }
    
    @Override
    public void annotate(AnnoSentenceCollection inputSents) {
        // Read a model from a file.
        log.info("Reading pruning model from file: " + pruneModel);
        JointNlpFgModel model = (JointNlpFgModel) Files.deserialize(pruneModel);
        
        ObsFeatureConjoiner ofc = model.getOfc();
        CorpusStatistics cs = model.getCs();
        JointNlpFeatureExtractorPrm fePrm = model.getFePrm();   

        // Get configuration for first-order pruning model.
        exPrm.fgPrm.includeSrl = false;
        exPrm.fgPrm.dpPrm = new DepParseFactorGraphBuilderPrm();
        exPrm.fgPrm.dpPrm.linkVarType = VarType.PREDICTED;
        exPrm.fgPrm.dpPrm.grandparentFactors = false;
        exPrm.fgPrm.dpPrm.siblingFactors = false;
        exPrm.fgPrm.dpPrm.unaryFactors = true;
        exPrm.fgPrm.dpPrm.useProjDepTreeFactor = true;
        exPrm.fgPrm.dpPrm.pruneEdges = true;
        exPrm.fePrm = fePrm;
        
        // Get unlabeled data.
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(exPrm, ofc, cs, false);
        FgExampleList data = builder.getData(inputSents, null);
        
        // Decode and create edge pruning mask.
        log.info("Running the pruning decoder.");
        int numEdgesTot = 0;
        int numEdgesKept = 0;
        Timer timer = new Timer();
        timer.start();
        // Add the new predictions to the input sentences.
        for (int i = 0; i < inputSents.size(); i++) {
            LFgExample ex = data.get(i);
            AnnoSentence inputSent = inputSents.get(i);
            // TODO: Because we use the JointNlpDecoder, we end up computing the MBR parse twice
            // (once for the mask, once for the parents array).
            JointNlpDecoder decoder = new JointNlpDecoder(dPrm);
            AnnoSentence predSent = decoder.decode(model, ex, inputSent);
            
            // Update the dependency tree on the sentence.
            int[] parents = predSent.getParents();
            if (parents != null) {
                inputSent.setParents(parents);
            }

            // Update the pruning mask.
            DepEdgeMask mask = predSent.getDepEdgeMask();
            if (mask != null) {
                if (inputSent.getDepEdgeMask() == null) {
                    inputSent.setDepEdgeMask(mask);
                } else {
                    inputSent.getDepEdgeMask().and(mask);
                }
            }
            numEdgesKept += mask.getCount();
            int n = predSent.getWords().size();
            numEdgesTot += n*n;                
        }
        timer.stop();
        log.info(String.format("Pruning decoded at %.2f tokens/sec", inputSents.getNumTokens() / timer.totSec()));
        int numEdgesPruned = numEdgesTot - numEdgesKept;
        log.info(String.format("Pruned %d / %d = %f edges", numEdgesPruned, numEdgesTot, (double) numEdgesPruned / numEdgesTot));  
    }

}
