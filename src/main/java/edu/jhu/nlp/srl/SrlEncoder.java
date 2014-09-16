package edu.jhu.nlp.srl;

import edu.jhu.gm.app.Encoder;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.LabeledFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.decode.MbrDecoder;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureCache;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepParseEncoder.DepParseEncoderPrm;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder;
import edu.jhu.nlp.depparse.DepParseFeatureExtractor;
import edu.jhu.nlp.joint.JointNlpFactorGraph;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SenseVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SrlFactorGraphBuilderPrm;
import edu.jhu.nlp.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;

/**
 * Encodes an {@link AnnoSentence} as a semantic role labeling factor graph and its training
 * variable assignment.
 * 
 * @author mgormley
 */
public class SrlEncoder implements Encoder<AnnoSentence, SrlGraph> {

    // TODO: Use this in JointNlp
    public static class SrlEncoderPrm {
        // TODO: Fill w/non-null values.
        public SrlFactorGraphBuilderPrm srlPrm = null; //new SrlFactorGraphBuilderPrm();
        public SrlFeatureExtractorPrm srlFePrm = null; //new SrlFeatureExtractorPrm();        
    }
    
    private SrlEncoderPrm prm;
    private CorpusStatistics cs;
    private ObsFeatureConjoiner ofc;
    
    public SrlEncoder(SrlEncoderPrm prm, CorpusStatistics cs, ObsFeatureConjoiner ofc) {
        this.prm = prm;
        this.cs = cs;
        this.ofc = ofc;
    }

    @Override
    public LFgExample encode(AnnoSentence sent, SrlGraph graph) {
        return getExample(sent, graph, true);
    }

    @Override
    public UFgExample encode(AnnoSentence sent) {
        return getExample(sent, null, false);
    }

    private LFgExample getExample(AnnoSentence sent, SrlGraph graph, boolean labeledExample) {
        // Create a feature extractor for this example.
        ObsFeatureExtractor obsFe = new SrlFeatureExtractor(prm.srlFePrm, sent, cs);
        obsFe = new ObsFeatureCache(obsFe);
        
        FactorGraph fg = new FactorGraph();
        SrlFactorGraphBuilder srl = new SrlFactorGraphBuilder(prm.srlPrm);
        srl.build(sent, cs, obsFe, ofc, fg);
        
        VarConfig goldConfig = new VarConfig();
        addSrlTrainAssignment(sent, graph, srl, goldConfig, prm.srlPrm.predictSense, prm.srlPrm.predictPredPos);

        FactorTemplateList fts = ofc.getTemplates();
        if (labeledExample) {
            return new LabeledFgExample(fg, goldConfig, obsFe, fts);
        } else {
            return new UnlabeledFgExample(fg, goldConfig, obsFe, fts);
        }
    }
    
    public static void addSrlTrainAssignment(AnnoSentence sent, SrlGraph srlGraph, SrlFactorGraphBuilder sfg, VarConfig vc, boolean predictSense, boolean predictPredPos) {        
        // ROLE VARS
        // Add all the training data assignments to the role variables, if they are not latent.
        // First, just set all the role names to "_".
        for (int i=0; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                RoleVar roleVar = sfg.getRoleVar(i, j);
                if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                    vc.put(roleVar, "_");
                }
            }
        }
        // Then set the ones which are observed.
        for (SrlEdge edge : srlGraph.getEdges()) {
            int parent = edge.getPred().getPosition();
            int child = edge.getArg().getPosition();
            String roleName = edge.getLabel();
            
            RoleVar roleVar = sfg.getRoleVar(parent, child);
            if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                int roleNameIdx = roleVar.getState(roleName);
                // TODO: This isn't quite right...we should really store the actual role name here.
                if (roleNameIdx == -1) {
                    vc.put(roleVar, CorpusStatistics.UNKNOWN_ROLE);
                } else {
                    vc.put(roleVar, roleNameIdx);
                }
            }
        }
        
        // Add the training data assignments to the predicate senses.
        for (int i=0; i<sent.size(); i++) {
            SenseVar senseVar = sfg.getSenseVar(i);
            if (senseVar != null) {
                if (predictSense) {
                    if (predictPredPos && !sent.isKnownPred(i)) {
                        vc.put(senseVar, "_");
                    } else {
                        // Tries to map the sense variable to its label (e.g. argM-TMP).
                        // If the variable state space does not include that label, we
                        // fall back on the UNKNOWN_SENSE constant. If for some reason
                        // the UNKNOWN_SENSE constant isn't present, we just predict the
                        // first possible sense.
                        if (!tryPut(vc, senseVar, srlGraph.getPredAt(i).getLabel())) {
                            if (!tryPut(vc, senseVar, CorpusStatistics.UNKNOWN_SENSE)) {
                                // This is a hack to ensure that something is added at test time.
                                vc.put(senseVar, 0);
                            }
                        }
                    }
                } else if (predictPredPos) {
                    if (sent.isKnownPred(i)) {
                        // We use CorpusStatistics.UNKNOWN_SENSE to indicate that
                        // there exists a predicate at this position.
                        vc.put(senseVar, CorpusStatistics.UNKNOWN_SENSE);
                    } else {
                        // The "_" indicates that there is no predicate at this
                        // position.
                        vc.put(senseVar, "_");
                    }
                } else {
                    throw new IllegalStateException("Neither predictSense nor predictPredPos is set. So there shouldn't be any SenseVars.");
                }
            }
        }
    }

    /**
     * Trys to put the entry (var, stateName) in vc.
     * @return True iff the entry (var, stateName) was added to vc.
     */
    private static boolean tryPut(VarConfig vc, Var var, String stateName) {
        int stateNameIdx = var.getState(stateName);
        if (stateNameIdx == -1) {
            return false;
        } else {
            vc.put(var, stateName);
            return true;
        }
    }
    
}
