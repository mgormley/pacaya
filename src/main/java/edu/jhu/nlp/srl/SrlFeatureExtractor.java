package edu.jhu.nlp.srl;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.ObsFeTypedFactor;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorTemplate;
import edu.jhu.nlp.features.SentFeatureExtractor;
import edu.jhu.nlp.features.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.nlp.joint.JointNlpFactorGraph.JointFactorTemplate;
import edu.jhu.nlp.relations.FeatureUtils;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SenseVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SrlFactorTemplate;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.Prm;

/**
 * Feature extractor for SRL. All the "real" feature extraction is done in
 * SentFeatureExtraction which considers only the observations.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class SrlFeatureExtractor implements ObsFeatureExtractor {

    public static class SrlFeatureExtractorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        /** Feature options. */
        public SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        /** The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled. */
        public int featureHashMod = -1;
        /** Whether to create human interpretable feature names when possible. */
        public boolean humanReadable = true;
    }
    
    private static final Logger log = Logger.getLogger(SrlFeatureExtractor.class); 
    
    private SrlFeatureExtractorPrm prm;
    private FactorTemplateList fts;
    private VarConfig obsConfig;
    private SentFeatureExtractor sentFeatExt;
    
    public SrlFeatureExtractor(SrlFeatureExtractorPrm prm, AnnoSentence sent, CorpusStatistics cs) {
        this.prm = prm;
        // TODO: SentFeatureExtractorCache uses a lot of memory storing lists of Strings. While this saves time when
        // SRL and dependency parsing use the same feature set, it's probably not worth the memory burden.
        //this.sentFeatExt = new SentFeatureExtractorCache(new SentFeatureExtractor(prm.fePrm, sent, cs));
        this.sentFeatExt = new SentFeatureExtractor(prm.fePrm, sent, cs);
    }

    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        this.obsConfig = ex.getObsConfig();
        this.fts = fts;
    }

    // For testing only.
    void init(VarConfig obsConfig, FactorTemplateList fts) {
        this.obsConfig = obsConfig;
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        ObsFeTypedFactor f = (ObsFeTypedFactor) factor;
        Enum<?> ft = f.getFactorType();
        VarSet vars = f.getVars();
        
        // Get the observation features.
        ArrayList<String> obsFeats;
        FeatureNames alphabet;
        if (ft == JointFactorTemplate.LINK_ROLE_BINARY || ft == DepParseFactorTemplate.LINK_UNARY 
                || ft == SrlFactorTemplate.ROLE_UNARY || ft == SrlFactorTemplate.SENSE_ROLE_BINARY) {
            // Look at the variables to determine the parent and child.
            Var var = vars.iterator().next();
            int parent;
            int child;
            if (var instanceof LinkVar) {
                parent = ((LinkVar)var).getParent();
                child = ((LinkVar)var).getChild();
            } else {
                parent = ((RoleVar)var).getParent();
                child = ((RoleVar)var).getChild();
            }

            // Get features on the observations for a pair of words.
            // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
            // 
            // As of 12/18/13, this breaks backwards compatibility with SOME of
            // the features in SentFeatureExtractor including useNarad and
            // useSimple.
            obsFeats = sentFeatExt.createFeatureSet(parent, child);
        } else if (ft == SrlFactorTemplate.SENSE_UNARY) {
            SenseVar var = (SenseVar) vars.iterator().next();
            int parent = var.getParent();
            obsFeats = sentFeatExt.createFeatureSet(parent);
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        alphabet = fts.getTemplate(f).getAlphabet();
        
        // Create prefix containing the states of the observed variables.
        String prefix = getObsVarsStates(f) + "_";
        
        if (log.isTraceEnabled()) {
            log.trace("Num obs features in factor: " + obsFeats.size());
        }
                
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        ArrayList<String> biasFeats = new ArrayList<String>();
        biasFeats.add("BIAS_FEATURE");
        if (!"_".equals(prefix)) {
            biasFeats.add(prefix + "BIAS_FEATURE");
        }
        
        // Add the bias features.
        FeatureVector fv = new FeatureVector(biasFeats.size() + obsFeats.size());
        FeatureUtils.addFeatures(biasFeats, alphabet, fv, true, prm.featureHashMod);
        
        // Add the other features.
        FeatureUtils.addPrefix(obsFeats, prefix);
        FeatureUtils.addFeatures(obsFeats, alphabet, fv, false, prm.featureHashMod);
        
        return fv;
    }

    /**
     * Gets a string representation of the states of the observed variables for
     * this factor.
     */
    private String getObsVarsStates(Factor f) {
        if (prm.humanReadable) {
            StringBuilder sb = new StringBuilder();
            int i=0;
            for (Var v : f.getVars()) {
                if (v.getType() == VarType.OBSERVED) {
                    if (i > 0) {
                        sb.append("_");
                    }
                    sb.append(obsConfig.getStateName(v));
                    i++;
                }
            }
            return sb.toString();
        } else {
            throw new RuntimeException("This is probably a bug. We should only be considering OBSERVED variables.");
            //return Integer.toString(goldConfig.getConfigIndexOfSubset(f.getVars()));
        }
    }
    
}