package edu.jhu.featurize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.featurize.SrlFactorGraph.RoleVar;
import edu.jhu.featurize.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.featurize.SrlFeatureExtractor.SrlFeatureExtractorPrm;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureExtractor;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
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
        public SrlFeatureExtractorPrm fePrm = new SrlFeatureExtractorPrm();
    }
    
    private final SrlFgExampleBuilderPrm prm;
    private final Alphabet<Feature> alphabet;
    private final CorpusStatistics cs;

    public SrlFgExampleBuilder(SrlFgExampleBuilderPrm prm, Alphabet<Feature> alphabet, CorpusStatistics cs) {
        this.prm = prm;
        this.alphabet = alphabet;
        this.cs = cs;
    }
    
    public FgExample getFGExample(CoNLL09Sentence sent) {
        // Precompute a few things.
        SrlGraph srlGraph = sent.getSrlGraph();
        
        // Tells us whether or not we should train on this.
        // TODO: This was never used. Should we remove it?
        boolean hasPred = false;
        Map<Pair<Integer,Integer>,String> knownPairs = new HashMap<Pair<Integer,Integer>,String>();
        List<SrlEdge> srlEdges = srlGraph.getEdges();
        Set<Integer> knownPreds = new HashSet<Integer>();
        // All the "Y"s
        for (SrlEdge e : srlEdges) {
            Integer a = e.getPred().getPosition();
            hasPred = true;
            knownPreds.add(a);
            // All the args for that Y.  Assigns one label for every arg the predicate selects for.
            for (SrlEdge e2 : e.getPred().getEdges()) {
                String[] splitRole = e2.getLabel().split("-");
                String role = splitRole[0].toLowerCase();
                Integer b = e2.getArg().getPosition();
                Pair<Integer, Integer> key = new Pair<Integer, Integer>(a, b);
                knownPairs.put(key, role);
            }
        }
        
        // Construct the factor graph.
        SrlFactorGraph sfg = new SrlFactorGraph(prm.fgPrm, sent, knownPreds, cs);        
        // Get the variable assignments given in the training data.
        VarConfig trainConfig = getTrainAssignment(sent, srlGraph, sfg);        
        // Create a feature extractor for this example.
        FeatureExtractor featExtractor = new SrlFeatureExtractor(prm.fePrm, sent, sfg, alphabet, cs);
        
        return new FgExample(sfg, trainConfig, featExtractor);
    }

    private VarConfig getTrainAssignment(CoNLL09Sentence sent, SrlGraph srlGraph, SrlFactorGraph sfg) {
        VarConfig vc = new VarConfig();

        // Add all the training data assignments to the link variables, if they are not latent.
        for (int i=0; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                if (j != i && sfg.getLinkVar(i, j) != null) {
                    LinkVar linkVar = sfg.getLinkVar(i, j);
                    if (linkVar.getType() != VarType.LATENT) {
                        // Syntactic head, from dependency parse.
                        int head = sent.get(j).getHead();
                        int state;
                        if (head != i) {
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
        for (SrlEdge edge : srlGraph.getEdges()) {
            int i = edge.getArg().getPosition();
            int j = edge.getPred().getPosition();
            String roleName = edge.getLabel();
            
            RoleVar roleVar = sfg.getRoleVar(i, j);
            if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                vc.put(roleVar, roleName);
            }
        }
                
        return vc;
    }
    
    
}
