package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.nlp.features.LocalObservations;
import edu.jhu.nlp.features.TemplateFeatureExtractor;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.util.Alphabet;

/**
 * Feature extraction for relations.
 * @author mgormley
 */
public class RelObsFe implements ObsFeatureExtractor {
    
    private RelationsFactorGraphBuilderPrm prm;
    private AnnoSentence sent;
    private FactorTemplateList fts;

    public RelObsFe(RelationsFactorGraphBuilderPrm prm, AnnoSentence sent, FactorTemplateList fts) {
        this.prm = prm;
        this.sent = sent;
        this.fts = fts;
    }
    
    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        return;
    }

    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        TemplateFeatureExtractor fe = new TemplateFeatureExtractor(sent, null);
        List<String> obsFeats = new ArrayList<>();
        RelVar rv = (RelVar) factor.getVars().get(0);
        LocalObservations local = LocalObservations.newNe1Ne2(rv.ment1, rv.ment2);
        fe.addFeatures(prm.templates, local, obsFeats);

        Alphabet<Feature> alphabet = fts.getTemplate(factor).getAlphabet();
        
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        ArrayList<String> biasFeats = new ArrayList<String>(1);
        biasFeats.add("BIAS_FEATURE");
        // Add the bias features.
        FeatureVector fv = new FeatureVector(1 + obsFeats.size());
        FeatureUtils.addFeatures(biasFeats, alphabet, fv, true, prm.featureHashMod);
        
        // Add the other features.
        FeatureUtils.addFeatures(obsFeats, alphabet, fv, false, prm.featureHashMod);
        
        return fv;
    }

    
}