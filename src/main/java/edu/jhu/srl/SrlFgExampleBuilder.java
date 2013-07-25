package edu.jhu.srl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.util.Alphabet;

/**
 * Factory of FgExamples for SRL.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SrlFgExampleBuilder {
    
    public static class SrlFgExampleBuilderPrm {
        public SrlFactorGraphPrm fgPrm = new SrlFactorGraphPrm();
        public SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        public SrlFeatureExtractorPrm srlFePrm = new SrlFeatureExtractorPrm();
    }
    
    private final SrlFgExampleBuilderPrm prm;
    private final Alphabet<Feature> alphabet;
    private final CorpusStatistics cs;
    private Alphabet<String> obsAlphabet;

    public SrlFgExampleBuilder(SrlFgExampleBuilderPrm prm, Alphabet<Feature> alphabet, CorpusStatistics cs, Alphabet<String> obsAlphabet) {
        this.prm = prm;
        this.alphabet = alphabet;
        this.cs = cs;
        this.obsAlphabet = obsAlphabet;
    }
    
    public FgExample getFGExample(CoNLL09Sentence sent) {
        // Precompute a few things.
        SrlGraph srlGraph = sent.getSrlGraph();
        
        Set<Integer> knownPreds = getKnownPreds(srlGraph);
        
        // Construct the factor graph.
        SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent.size(), knownPreds, cs.roleStateNames);        
        // Get the variable assignments given in the training data.
        VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);        
        // Create a feature extractor for this example.
        SentFeatureExtractor sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs, obsAlphabet);
        FeatureExtractor featExtractor = new SrlFeatureExtractor(prm.srlFePrm, sfg, alphabet, sentFeatExt);
        
        return new FgExample(sfg, trainConfig, featExtractor);
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
        int[] parents = prm.fePrm.useGoldSyntax ? sent.getParentsFromHead() : sent.getParentsFromPhead();
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
                vc.put(roleVar, roleName);
            }
        }
                
        return vc;
    }
    
    
}
