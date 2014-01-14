package edu.jhu.srl;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.featurize.SentFeatureExtractorCache;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactor;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prm;
import edu.jhu.util.hash.MurmurHash3;

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
    private SrlFactorGraph sfg;
    private FactorTemplateList fts;
    private VarConfig goldConfig;
    private SentFeatureExtractorCache sentFeatExt;
        
    public SrlFeatureExtractor(SrlFeatureExtractorPrm prm, SimpleAnnoSentence sent, CorpusStatistics cs) {
        this.prm = prm;
        this.sentFeatExt = new SentFeatureExtractorCache(new SentFeatureExtractor(prm.fePrm, sent, cs));
    }

    @Override
    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig,
            FactorTemplateList fts) {
        this.sfg = (SrlFactorGraph) fg;
        this.goldConfig = goldConfig;
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(int factorId) {
        SrlFactor f = (SrlFactor) sfg.getFactor(factorId);
        SrlFactorTemplate ft = f.getFactorType();
        VarSet vars = f.getVars();
        
        // Get the observation features.
        ArrayList<String> obsFeats;
        Alphabet<Feature> alphabet;
        if (ft == SrlFactorTemplate.LINK_ROLE_BINARY || ft == SrlFactorTemplate.LINK_UNARY || ft == SrlFactorTemplate.ROLE_UNARY) {
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
            obsFeats = sentFeatExt.fastGetObsFeats(parent, child);
        } else if (ft == SrlFactorTemplate.SENSE_UNARY) {
            SenseVar var = (SenseVar) vars.iterator().next();
            int parent = var.getParent();
            obsFeats = sentFeatExt.fastGetObsFeats(parent);
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        alphabet = fts.getTemplate(f).getAlphabet();
        
        // Create prefix containing the states of the observed variables.
        String prefix = getObsVarsStates(f) + "_";
        
        if (log.isTraceEnabled()) {
            log.trace("Num obs features in factor: " + obsFeats.size());
        }
        
        FeatureVector fv = new FeatureVector(obsFeats.size());
        
        // Add the bias features.
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        ArrayList<String> biasFeats = new ArrayList<String>();
        biasFeats.add("BIAS_FEATURE");
        if (!"_".equals(prefix)) {
            biasFeats.add(prefix + "BIAS_FEATURE");
        }
        addFeatures(biasFeats, alphabet, "", fv, true);
        
        // Add the other features.
        addFeatures(obsFeats, alphabet, prefix, fv, false);
        
        return fv;
    }

    /**
     * Prepends the string prefix to each feature in obsFeats, and adds each one to fv using the given alphabet.
     */
    private void addFeatures(ArrayList<String> obsFeats, Alphabet<Feature> alphabet, String prefix, FeatureVector fv, boolean isBiasFeat) {
        if (prm.featureHashMod <= 0) {
            // Just use the features as-is.
            for (String obsFeat : obsFeats) {
                String fname = prefix + obsFeat;
                int fidx = alphabet.lookupIndex(new Feature(fname, isBiasFeat));
                if (fidx != -1) {
                    fv.add(fidx, 1.0);
                }
            }
        } else {
            // Apply the feature-hashing trick.
            for (String obsFeat : obsFeats) {
                String fname = prefix + obsFeat;
                int hash = MurmurHash3.murmurhash3_x86_32(fname, 0, fname.length(), 123456789);
                hash = FastMath.mod(hash, prm.featureHashMod);
                int fidx = alphabet.lookupIndex(new Feature(hash, isBiasFeat));
                if (fidx != -1) {
                    int revHash = reverseHashCode(fname);
                    if (revHash < 0) {
                        fv.add(fidx, -1.0);
                    } else {
                        fv.add(fidx, 1.0);
                    }
                }
            }
        }
    }

    /**
     * Gets a string representation of the states of the observed variables for
     * this factor.
     */
    private String getObsVarsStates(SrlFactor f) {
        if (prm.humanReadable) {
            StringBuilder sb = new StringBuilder();
            int i=0;
            for (Var v : f.getVars()) {
                if (i > 0) {
                    sb.append("_");
                }
                if (v.getType() == VarType.OBSERVED) {
                    sb.append(goldConfig.getStateName(v));
                    i++;
                }
            }
            return sb.toString();
        } else {
            return Integer.toString(goldConfig.getConfigIndexOfSubset(f.getVars()));
        }
    }

    /**
     * Returns the hash code of the reverse of this string.
     */
    private int reverseHashCode(String fname) {
        int hash = 0;
        int n = fname.length();
        for (int i=n-1; i>=0; i--) {
            hash += 31 * hash + fname.charAt(i);
        }
        return hash;
    }

    @Override
    public void clear() {
        sentFeatExt.clear();
    }
    
}