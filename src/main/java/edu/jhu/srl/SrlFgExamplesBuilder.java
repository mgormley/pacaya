package edu.jhu.srl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.gm.data.AbstractFgExampleList;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder;
import edu.jhu.gm.data.FgExampleListBuilder.FgExamplesBuilderPrm;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;

/**
 * Factory for SRL FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SrlFgExamplesBuilder {

    public static class SrlFgExampleBuilderPrm {
        public SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        public SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();
        public FgExamplesBuilderPrm exPrm = new FgExamplesBuilderPrm();
    }
    
    private static final Logger log = Logger.getLogger(SrlFgExamplesBuilder.class);

    private ObsFeatureConjoiner ofc;
    private SrlFgExampleBuilderPrm prm;
    private CorpusStatistics cs;
    private FactorTemplateList fts;

    public SrlFgExamplesBuilder(SrlFgExampleBuilderPrm prm, ObsFeatureConjoiner ofc, CorpusStatistics cs) {
        this.prm = prm;
        this.ofc = ofc;
        this.cs = cs;
        this.fts = ofc.getTemplates();
        //this.sents = sents;
    }

    public FgExampleList getData(SimpleAnnoSentenceCollection sents) {
        FgExampleListBuilder builder = new FgExampleListBuilder(prm.exPrm);
        FgExampleList data = builder.getInstance(new SrlFgExampleFactory(sents, ofc));
        data.setSourceSentences(sents);
        return data;
    }
    
    /** 
     * This class is read-only and thread-safe.
     */
    private class SrlFgExampleFactory extends AbstractFgExampleList implements FgExampleList {

        private SimpleAnnoSentenceCollection sents;
        private ObsFeatureConjoiner ofc;
        
        public SrlFgExampleFactory(SimpleAnnoSentenceCollection sents, ObsFeatureConjoiner ofc) {
            this.sents = sents;
            this.ofc = ofc;
        }
        
        public FgExample get(int i) {
            SimpleAnnoSentence sent = sents.get(i);
            
            // Precompute a few things.
            SrlGraph srlGraph = sent.getSrlGraph();
            
            Set<Integer> knownPreds = getKnownPreds(srlGraph);
            
            // Create a feature extractor for this example.
            ObsFeatureExtractor obsFe = new SrlFeatureExtractor(prm.srlFePrm, sent, cs);
            obsFe = new ObsFeatureCache(obsFe);
            
            // Construct the factor graph.
            SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent, knownPreds, cs, obsFe, ofc);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);
            
            // Create the example.
            FgExample ex = new FgExample(sfg, trainConfig, obsFe, fts);
            
            return ex;
        }
        
        public int size() {
            return sents.size();
        }

    }
    
    private static Set<Integer> getKnownPreds(SrlGraph srlGraph) {
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlPred pred : srlGraph.getPreds()) {
            knownPreds.add(pred.getPosition());
        }
        return knownPreds;
    }

    private static VarConfig getTrainAssignment(SimpleAnnoSentence sent, SrlGraph srlGraph, SrlFactorGraph sfg) {
        VarConfig vc = new VarConfig();

        // LINK VARS
        // Add all the training data assignments to the link variables, if they are not latent.
        // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
        int[] parents = sent.getParents();
        for (int i=-1; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                if (j != i && sfg.getLinkVar(i, j) != null) {
                    LinkVar linkVar = sfg.getLinkVar(i, j);
                    if (linkVar.getType() != VarType.LATENT) {
                        // Syntactic head, from dependency parse.
                        int state;
                        if (parents[j] != i) {
                            state = LinkVar.FALSE;
                        } else {
                            state = LinkVar.TRUE;
                        }
                        vc.put(linkVar, state);
                    }
                }
            }
        }
        
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
        }
        return vc;
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
