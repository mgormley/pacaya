package edu.jhu.srl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.featurize.FeaturizedSentence;
import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.featurize.TemplateFeatureExtractor;
import edu.jhu.featurize.TemplateSets;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.featurize.TemplateFeatureExtractor.LocalObservations;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.DepParseFactorGraph.DepParseFactorTemplate;
import edu.jhu.srl.DepParseFactorGraph.O2FeTypedFactor;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prm;
import edu.jhu.util.hash.MurmurHash3;

public class DepParseFeatureExtractor implements FeatureExtractor {

    public static class DepParseFeatureExtractorPrm extends Prm {
        private static final long serialVersionUID = 1L;
        /** Feature options. */
        public List<FeatTemplate> firstOrderTpls = TemplateSets.getNaradowskyArgUnigramFeatureTemplates();
        public List<FeatTemplate> secondOrderTpls = TemplateSets.getFromResource(TemplateSets.carreras07Dep2FeatsResource);
        public boolean biasOnly = false;
        /** The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled. */
        public int featureHashMod = -1;
        /** Whether to create human interpretable feature names when possible. */
        public boolean humanReadable = true;
    }
    
    private static final Logger log = Logger.getLogger(DepParseFeatureExtractor.class); 
    
    private DepParseFeatureExtractorPrm prm;
    private VarConfig goldConfig;
    private Alphabet<Object> alphabet;
    private TemplateFeatureExtractor ext;
    
    public DepParseFeatureExtractor(DepParseFeatureExtractorPrm prm, SimpleAnnoSentence sent, CorpusStatistics cs, Alphabet<Object> alphabet) {
        this.prm = prm;
        FeaturizedSentence fSent = new FeaturizedSentence(sent, cs);
        ext = new TemplateFeatureExtractor(fSent, cs);
        this.alphabet = alphabet;
    }

    @Override
    public void init(FgExample ex) {
        this.goldConfig = ex.getGoldConfig();
    }
    
    private final FeatureVector emptyFv = new FeatureVector();

    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        FeTypedFactor f = (FeTypedFactor) factor;
        Enum<?> ft = f.getFactorType();
        VarSet vars = f.getVars();
        
        int[] vc = vars.getVarConfigAsArray(configId);
        if (ArrayUtils.contains(vc, LinkVar.FALSE)) {
            return emptyFv;
        }
                
        // Get the observation features.
        ArrayList<String> obsFeats = new ArrayList<String>();
        if (ft == DepParseFactorTemplate.LINK_UNARY) {
            // Look at the variables to determine the parent and child.
            LinkVar var = (LinkVar) vars.get(0);
            int pidx = var.getParent();
            int cidx = var.getChild();
            ext.addFeatures(prm.firstOrderTpls, LocalObservations.newPidxCidx(pidx, cidx), obsFeats);
        } else if (ft == DepParseFactorTemplate.LINK_GRANDPARENT || ft == DepParseFactorTemplate.LINK_SIBLING) {
            O2FeTypedFactor f2 = (O2FeTypedFactor)f;
            ext.addFeatures(prm.secondOrderTpls, LocalObservations.newPidxCidxMidx(f2.i, f2.j, f2.k), obsFeats);
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        
        // NOTE: We don't need to append the state of the LinkVars since they are all True.
        // Create prefix containing the states of the observed variables.
        String prefix = ft + "_" + getObsVarsStates(f) + "_";
                
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
    private void addFeatures(ArrayList<String> obsFeats, Alphabet<Object> alphabet, String prefix, FeatureVector fv, boolean isBiasFeat) {
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
    private String getObsVarsStates(Factor f) {
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
            throw new RuntimeException("This is probably a bug. We should only be considering OBSERVED variables.");
            //return Integer.toString(goldConfig.getConfigIndexOfSubset(f.getVars()));
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
}
