package edu.jhu.srl;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplate;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.ObsFeatureExtractor;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;
import edu.jhu.util.Timer;

/**
 * Factory for FgExamples.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SrlFgExamplesBuilder {

    public static class SrlFgExampleBuilderPrm {
        /* These provide default values during testing; otherwise, 
         * values should be defined by SrlRunner. */
        public SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        public SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        public SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();
        /**
         * Minimum number of times (inclusive) a feature must occur in training
         * to be included in the model. Ignored if non-positive. (Using this
         * cutoff implies that unsupported features will not be included.)
         */
        public int featCountCutoff = -1;
    }
    
    private static final Logger log = Logger.getLogger(SrlFgExamplesBuilder.class);

    private FeatureTemplateList fts;
    private SrlFgExampleBuilderPrm prm;
    private CorpusStatistics cs;
    private Timer fgTimer = new Timer();

    
    public SrlFgExamplesBuilder(SrlFgExampleBuilderPrm prm, FeatureTemplateList fts, CorpusStatistics cs) {
        this.prm = prm;
        this.fts = fts;
        this.cs = cs;
    }
    
    public void preprocess(SimpleAnnoSentenceCollection sents) {
        if (!(fts.isGrowing() && prm.featCountCutoff > 0)) {
            // Skip this preprocessing step since it will have no effect.
            return;
        }
        
        // Use counting alphabets in this ftl.
        FeatureTemplateList counter = new FeatureTemplateList(true);
        for (int i=0; i<sents.size(); i++) {
            SimpleAnnoSentence sent = sents.get(i);
            if (i % 1000 == 0 && i > 0) {
                log.debug("Preprocessed " + i + " examples...");
            }
            
            fgTimer.start();
            // Precompute a few things.
            SrlGraph srlGraph = sent.getSrlGraph();
            
            Set<Integer> knownPreds = getKnownPreds(srlGraph);
            
            // Construct the factor graph.
            SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent, knownPreds, cs);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);

            // Create a feature extractor for this example.
            SentFeatureExtractor sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs);
            ObsFeatureExtractor featExtractor = new SrlFeatureExtractor(prm.srlFePrm, sentFeatExt);
            fgTimer.stop();            
            
            // Create the example solely to count the features.
            new FgExample(sfg, trainConfig, featExtractor, counter);
        }
        
        for (int t=0; t<counter.size(); t++) {            
            FeatureTemplate template = counter.get(t);
            CountingAlphabet<Feature> countAlphabet = (CountingAlphabet<Feature>) template.getAlphabet();
            
            // Create a copy of this template, with a new alphabet.
            Alphabet<Feature> alphabet = new Alphabet<Feature>();
            fts.add(new FeatureTemplate(template.getVars(), alphabet, template.getKey()));

            // Discard the features which occurred fewer times than the cutoff.
            for (int i=0; i<countAlphabet.size(); i++) {
                int count = countAlphabet.lookupObjectCount(i);
                Feature feat = countAlphabet.lookupObject(i);
                if (count >= prm.featCountCutoff || feat.isBiasFeature()) {
                    alphabet.lookupIndex(feat);
                }
            }
            alphabet.stopGrowth();
        }
    }

    public FgExamples getData(SimpleAnnoSentenceCollection sents) {
        preprocess(sents);
        
        FgExamples data = new FgExamples(fts);
        for (int i=0; i<sents.size(); i++) {
            SimpleAnnoSentence sent = sents.get(i);
            if (i % 1000 == 0 && i > 0) {
                log.debug("Built " + i + " examples...");
            }
            
            fgTimer.start();
            // Precompute a few things.
            SrlGraph srlGraph = sent.getSrlGraph();
            
            Set<Integer> knownPreds = getKnownPreds(srlGraph);
            
            // Construct the factor graph.
            SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent, knownPreds, cs);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);

            // Create a feature extractor for this example.
            SentFeatureExtractor sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs);
            ObsFeatureExtractor featExtractor = new SrlFeatureExtractor(prm.srlFePrm, sentFeatExt);
            fgTimer.stop();            
            
            FgExample ex = new FgExample(sfg, trainConfig, featExtractor, fts);
            data.add(ex);
        }
        
        log.info("Time (ms) to construct factor graph: " + fgTimer.totMs());
        log.info("Time (ms) to clamp factor graphs: " + data.getTotMsFgClampTimer());
        log.info("Time (ms) to cache features: " + data.getTotMsFeatCacheTimer());
        
        data.setSourceSentences(sents);
        return data;
    }
    
    private static Set<Integer> getKnownPreds(SrlGraph srlGraph) {
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlPred pred : srlGraph.getPreds()) {
            knownPreds.add(pred.getPosition());
        }
        return knownPreds;
    }

    private VarConfig getTrainAssignment(SimpleAnnoSentence sent, SrlGraph srlGraph, SrlFactorGraph sfg) {
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
    private boolean tryPut(VarConfig vc, Var var, String stateName) {
        int stateNameIdx = var.getState(stateName);
        if (stateNameIdx == -1) {
            return false;
        } else {
            vc.put(var, stateName);
            return true;
        }
    }
}
