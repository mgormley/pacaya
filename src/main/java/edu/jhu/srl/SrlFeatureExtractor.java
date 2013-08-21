package edu.jhu.srl;

import org.apache.log4j.Logger;

import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.ObsFeatureExtractor;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FeatureVectorBuilder;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.Var;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactor;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;

/**
 * Feature extractor for SRL. All the "real" feature extraction is done in
 * SentFeatureExtraction which considers only the observations.
 * 
 * @author mgormley
 */
public class SrlFeatureExtractor implements ObsFeatureExtractor {

    public static class SrlFeatureExtractorPrm {
        /** The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled. */
        public int featureHashMod = -1;
        /** Whether to create human interpretable feature names when possible. */
        public boolean humanReadable = true;
    }
    
    private static final Logger log = Logger.getLogger(SrlFeatureExtractor.class); 
    
    private SrlFeatureExtractorPrm prm;
    
    // TODO: Move these caches to a separate wrapper class for SentFeatureExtractor.
    // Cache of observation features for each single positions in the sentence.
    private BinaryStrFVBuilder[] obsFeatsSolo;
    // Cache of observation features for each pair of positions in the sentence.
    private BinaryStrFVBuilder[][] obsFeatsPair;
    
    // -- Inputs --
    private SrlFactorGraph sfg;
    private FeatureTemplateList fts;
    private SentFeatureExtractor sentFeatExt;
    private VarConfig goldConfig;
    
    // A single alphabet for all the observed features.
    private Alphabet<String> obsAlphabet;
    
    public SrlFeatureExtractor(SrlFeatureExtractorPrm prm, SentFeatureExtractor sentFeatExt) {
        this(prm, sentFeatExt, new Alphabet<String>());
    }
    
    public SrlFeatureExtractor(SrlFeatureExtractorPrm prm, SentFeatureExtractor sentFeatExt, Alphabet<String> obsAlphabet) {
        this.prm = prm;
        this.sentFeatExt = sentFeatExt;
        obsFeatsSolo = new BinaryStrFVBuilder[sentFeatExt.getSentSize()];
        obsFeatsPair = new BinaryStrFVBuilder[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
        this.obsAlphabet = obsAlphabet;
    }

    @Override
    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig,
            FeatureTemplateList fts) {
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
        BinaryStrFVBuilder obsFeats;
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
            
            // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
            if (parent == -1) {
                obsFeats = fastGetObsFeats(child, obsAlphabet);
            } else {
                // Get features on the observations for a pair of words.
                obsFeats = fastGetObsFeats(parent, child, obsAlphabet);
            }
            alphabet = fts.getTemplate(f).getAlphabet();
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        
        // Create prefix containing the states of the observed variables.
        String prefix = getObsVarsStates(f);
        
        if (log.isTraceEnabled()) {
            log.trace("Num obs features in factor: " + obsFeats.size());
        }
        
        FeatureVector fv = new FeatureVector(obsFeats.size());
        
        // Add the bias features.
        // The bias features are used to ensure that at least one feature fires for each variable configuration.
        BinaryStrFVBuilder biasFeats = new BinaryStrFVBuilder(new Alphabet<String>());
        biasFeats.add("BIAS_FEATURE");
        biasFeats.add(prefix + "BIAS_FEATURE");
        addFeatures(biasFeats, alphabet, "", fv, true);
        
        // Add the other features.
        prefix += "_";
        addFeatures(obsFeats, alphabet, prefix, fv, false);
        
        return fv;
    }

    /**
     * Prepends the string prefix to each feature in obsFeats, and adds each one to fv using the given alphabet.
     */
    private void addFeatures(BinaryStrFVBuilder obsFeats, Alphabet<Feature> alphabet, String prefix, FeatureVector fv, boolean isBiasFeat) {
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
            FeatureVectorBuilder fvb = obsFeats.getFvb();
            for (IntDoubleEntry obsFeat : fvb) {
                // Using the fvb makes unreadable feature names, but is faster.
                String fname = prefix + Integer.toString(obsFeat.index());
                int hash = fname.hashCode();
                hash = hash % prm.featureHashMod;
                if (hash < 0) {
                    hash += prm.featureHashMod;
                }
                fname = Integer.toString(hash);
                int fidx = alphabet.lookupIndex(new Feature(fname, isBiasFeat));
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

    private BinaryStrFVBuilder fastGetObsFeats(int child, Alphabet<String> obsAlphabet) {
        if (obsFeatsSolo[child] == null) {
            // Lazily construct the observation features.
            obsFeatsSolo[child] = sentFeatExt.createFeatureSet(child, obsAlphabet);
        }
        return obsFeatsSolo[child];
    }

    private BinaryStrFVBuilder fastGetObsFeats(int parent, int child, Alphabet<String> obsAlphabet) {
        if (obsFeatsPair[parent][child] == null) {
            // Lazily construct the observation features.
            obsFeatsPair[parent][child] = sentFeatExt.createFeatureSet(parent, child, obsAlphabet);
        }
        return obsFeatsPair[parent][child];
    }
    
}