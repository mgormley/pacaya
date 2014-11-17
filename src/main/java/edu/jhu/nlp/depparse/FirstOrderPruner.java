package edu.jhu.nlp.depparse;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.nlp.joint.JointNlpDecoder.JointNlpDecoderPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder;
import edu.jhu.nlp.joint.JointNlpFgExamplesBuilder.JointNlpFgExampleBuilderPrm;
import edu.jhu.nlp.joint.JointNlpFgModel;
import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.util.Lambda.FnIntToVoid;
import edu.jhu.util.Prm;
import edu.jhu.util.Threads;
import edu.jhu.util.Timer;
import edu.jhu.util.files.Files;
import edu.jhu.util.semiring.Algebras;

public class FirstOrderPruner implements Annotator {

    private static final Logger log = Logger.getLogger(FirstOrderPruner.class);
    private File pruneModel;
    private JointNlpFgExampleBuilderPrm exPrm;
    private JointNlpDecoderPrm dPrm;

    public FirstOrderPruner(File pruneModel, JointNlpFgExampleBuilderPrm exPrm, JointNlpDecoderPrm dPrm) {
        this.pruneModel = pruneModel;
        this.exPrm = Prm.clone(exPrm);
        this.dPrm = dPrm;
    }
    
    @Override
    public void annotate(final AnnoSentenceCollection inputSents) {
        // Read a model from a file.
        log.info("Reading pruning model from file: " + pruneModel);
        final JointNlpFgModel model = (JointNlpFgModel) Files.deserialize(pruneModel);
        
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
        // TODO: This should only be true if we already ran a pruner before this one.
        exPrm.fgPrm.dpPrm.pruneEdges = true;
        exPrm.fePrm = fePrm;
                
        final ErmaBpPrm bpPrm = new ErmaBpPrm();
        bpPrm.s = Algebras.LOG_SEMIRING;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;
        bpPrm.convergenceThreshold = 0;
        bpPrm.keepTape = false;
        
        // Get unlabeled data.
        JointNlpFgExamplesBuilder builder = new JointNlpFgExamplesBuilder(exPrm, ofc, cs, false);
        final FgExampleList data = builder.getData(inputSents, null);
        
        // Decode and create edge pruning mask.
        log.info("Running the pruning decoder.");
        final AtomicInteger numEdgesTot = new AtomicInteger(0);
        final AtomicInteger numEdgesKept = new AtomicInteger(0);
        Timer timer = new Timer();
        timer.start();
        Threads.forEach(0, inputSents.size(), new FnIntToVoid() {            
            @Override
            public void call(int i) {
                try {
                    LFgExample ex = data.get(i);
                    AnnoSentence inputSent = inputSents.get(i);
                    
                    // Decode.
                    DepEdgeMaskDecoder decoder = new DepEdgeMaskDecoder(dPrm.maskPrm);
                    FactorGraph fgLatPred = ex.getFgLatPred();
                    fgLatPred.updateFromModel(model);
                    FgInferencer infLatPred = bpPrm.getInferencer(fgLatPred);
                    infLatPred.run();
                    DepEdgeMask mask = decoder.decode(infLatPred, ex, inputSent);
                    
                    // Update the pruning mask.
                    if (mask != null) {
                        if (inputSent.getDepEdgeMask() == null) {
                            inputSent.setDepEdgeMask(mask);
                        } else {
                            inputSent.getDepEdgeMask().and(mask);
                        }
                    }
                    numEdgesKept.addAndGet(mask.getCount());
                    int n = inputSent.getWords().size();
                    numEdgesTot.addAndGet(n*n);
                } catch (Throwable t) {
                    // TODO: Maybe move this elsewhere.
                    log.error("Caught throwable: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        });
        timer.stop();
        log.info(String.format("Pruning decoded at %.2f tokens/sec", inputSents.getNumTokens() / timer.totSec()));
        int numEdgesPruned = numEdgesTot.get() - numEdgesKept.get();
        log.info(String.format("Pruned %d / %d = %f edges", numEdgesPruned, numEdgesTot.get(), 
                (double) numEdgesPruned / numEdgesTot.get()));  
    }

}
