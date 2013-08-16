package edu.jhu.srl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.jhu.data.concrete.SimpleAnnoSentenceCollection;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;

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
        /** Whether to include unsupported features. */
        public boolean includeUnsupportedFeatures = false;
        /**
         * Minimum number of times (inclusive) a feature must occur in training
         * to be included in the model. Ignored if non-positive. (Using this
         * cutoff implies that unsupported features will not be included.)
         */
        public int featCountCutoff = -1;
    }
    
    private static final Logger log = Logger.getLogger(SrlFgExamplesBuilder.class);

    private SrlFgExampleBuilderPrm prm;
    private Alphabet<Feature> alphabet;
    private CorpusStatistics cs;
    
    public SrlFgExamplesBuilder(SrlFgExampleBuilderPrm prm, Alphabet<Feature> alphabet, CorpusStatistics cs) {
        this.prm = prm;
        this.alphabet = alphabet;
        this.cs = cs;
    }

    public FgExamples getData(SimpleAnnoSentenceCollection sents) {
        throw new RuntimeException("Not implemented");
    }
    
    public FgExamples getData(CoNLL09FileReader reader) {
        List<CoNLL09Sentence> sents = reader.readAll();
        return getData(sents);
    }
    
    
    public void preprocess(List<CoNLL09Sentence> sents) {
        if (!(alphabet.isGrowing() && prm.featCountCutoff > 0)) {
            // Skip this preprocessing step since it will have no effect.
            return;
        }
        
        CountingAlphabet<Feature> counter = new CountingAlphabet<Feature>();
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        List<FeatureExtractor> featExts = new ArrayList<FeatureExtractor>();
        for (int i=0; i<sents.size(); i++) {
            CoNLL09Sentence sent = sents.get(i);
            if (i % 1000 == 0 && i > 0) {
                log.debug("Built " + i + " examples...");
            }
            
            // Precompute a few things.
            SrlGraph srlGraph = sent.getSrlGraph();
            
            Set<Integer> knownPreds = getKnownPreds(srlGraph);
            
            // Construct the factor graph.
            SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent.size(), knownPreds, cs.roleStateNames);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);

            FgExample ex = new FgExample(sfg, trainConfig);
            
            // Create a feature extractor for this example.
            SentFeatureExtractor sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs, obsAlphabet);
            FeatureExtractor featExtractor = new SrlFeatureExtractor(prm.srlFePrm, sfg, counter, sentFeatExt);
            // So we don't have to compute the features again for this example.
            featExts.add(featExtractor);
            
            // Cache only the features observed in training data.
            ex.cacheLatFeats(sfg, trainConfig, featExtractor);
        }
        
        for (int i=0; i<counter.size(); i++) {
            int count = counter.lookupObjectCount(i);
            Feature feat = counter.lookupObject(i);
            if (count >= prm.featCountCutoff || feat.isBiasFeature()) {
                alphabet.lookupIndex(feat);
            }
        }
        alphabet.stopGrowth();
    }

    public FgExamples getData(List<CoNLL09Sentence> sents) {
        preprocess(sents);
        
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        List<FeatureExtractor> featExts = new ArrayList<FeatureExtractor>();
        
        FgExamples data = new FgExamples(alphabet);
        for (int i=0; i<sents.size(); i++) {
            CoNLL09Sentence sent = sents.get(i);
            if (i % 1000 == 0 && i > 0) {
                log.debug("Built " + i + " examples...");
            }
            
            // Precompute a few things.
            SrlGraph srlGraph = sent.getSrlGraph();
            
            Set<Integer> knownPreds = getKnownPreds(srlGraph);
            
            // Construct the factor graph.
            SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent.size(), knownPreds, cs.roleStateNames);        
            // Get the variable assignments given in the training data.
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);

            FgExample ex = new FgExample(sfg, trainConfig);
            
            // Create a feature extractor for this example.
            SentFeatureExtractor sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs, obsAlphabet);
            FeatureExtractor featExtractor = new SrlFeatureExtractor(prm.srlFePrm, sfg, alphabet, sentFeatExt);
            // So we don't have to compute the features again for this example.
            featExts.add(featExtractor);
            
            // Cache only the features observed in training data.
            ex.cacheLatFeats(sfg, trainConfig, featExtractor);
            data.add(ex);
        }
        
        if (!prm.includeUnsupportedFeatures) {
            alphabet.stopGrowth();
        }

        // Cache features for all the other variable assignments.
        for (int i=0; i<data.size(); i++) {
            if (i % 1000 == 0 && i > 0) {
                log.debug("Cached features for " + i + " examples...");
            }
            FgExample ex = data.get(i);
            CoNLL09Sentence sent = sents.get(i);
            SrlGraph srlGraph = sent.getSrlGraph();
            SrlFactorGraph sfg = (SrlFactorGraph) ex.getOriginalFactorGraph();
            VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);
            FeatureExtractor featExtractor = featExts.get(i);
            ex.cacheLatPredFeats(sfg, trainConfig, featExtractor);
        }

        log.info("Num observation functions: " + obsAlphabet.size());
        data.setSourceSentences(sents);
        return data;
    }
    
    private static Set<Integer> getKnownPreds(SrlGraph srlGraph) {
        List<SrlEdge> srlEdges = srlGraph.getEdges();
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlEdge e : srlEdges) {
            Integer a = e.getPred().getPosition();
            knownPreds.add(a);
        }
        return knownPreds;
    }

    private VarConfig getTrainAssignment(CoNLL09Sentence sent, SrlGraph srlGraph, SrlFactorGraph sfg) {
        VarConfig vc = new VarConfig();

        // Add all the training data assignments to the link variables, if they are not latent.
        //
        // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
        int[] parents = cs.prm.useGoldSyntax ? sent.getParentsFromHead() : sent.getParentsFromPhead();
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
                
        return vc;
    }
    
}
