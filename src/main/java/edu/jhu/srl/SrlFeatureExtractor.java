package edu.jhu.srl;

import org.apache.log4j.Logger;

import edu.jhu.featurize.SentFeatureExtractor;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.gm.CrfFeatureExtractor;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FeatureTemplateList;
import edu.jhu.gm.FeatureVector;
import edu.jhu.gm.FeatureVectorBuilder;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
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
public class SrlFeatureExtractor implements CrfFeatureExtractor {

    public static class SrlFeatureExtractorPrm {
        /** The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled. */
        public int featureHashMod = 524288; // 2^19
    }
    
    private static final Logger log = Logger.getLogger(SrlFeatureExtractor.class); 
    
    private SrlFeatureExtractorPrm prm;
    
    // Cache of observation features for each single positions in the sentence.
    private BinaryStrFVBuilder[] obsFeatsSolo;
    // Cache of observation features for each pair of positions in the sentence.
    private BinaryStrFVBuilder[][] obsFeatsPair;
    
    // -- Inputs --
    private SrlFactorGraph sfg;
    private final FeatureTemplateList fts;
    private SentFeatureExtractor sentFeatExt;

    // A single alphabet for all the observed features.
    private Alphabet<String> obsAlphabet;
    
    public SrlFeatureExtractor(SrlFeatureExtractorPrm prm, SrlFactorGraph sfg, FeatureTemplateList fts, SentFeatureExtractor sentFeatExt) {
        super();
        this.prm = prm;
        this.sfg = sfg;
        this.fts = fts;
        this.sentFeatExt = sentFeatExt;
        obsFeatsSolo = new BinaryStrFVBuilder[sentFeatExt.getSentSize()];
        obsFeatsPair = new BinaryStrFVBuilder[sentFeatExt.getSentSize()][sentFeatExt.getSentSize()];
        this.obsAlphabet = new Alphabet<String>();
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
            alphabet = fts.lookupTemplate(f).getAlphabet();
        } else {
            throw new RuntimeException("Unsupported template: " + ft);
        }
        
        FeatureVector fv = new FeatureVector(obsFeats.size());
        if (log.isTraceEnabled()) {
            log.trace("Num obs features in factor: " + obsFeats.size());
        }

        // TODO: We should really be ensuring that at least one feature (i.e.
        // the bias feature) fire for each variable configuration.
        if (prm.featureHashMod <= 0) {
            // Just use the features as-is.
            for (String obsFeat : obsFeats) {
                int fidx = alphabet.lookupIndex(new Feature(obsFeat));
                if (fidx != -1) {
                    fv.add(fidx, 1.0);
                }
            }
        } else {
            // Apply the feature-hashing trick.
            FeatureVectorBuilder fvb = obsFeats.getFvb();
            for (IntDoubleEntry obsFeat : fvb) {
                // Using the fvb makes unreadable feature names, but is faster.
                String fname = Integer.toString(obsFeat.index());
                int hash = fname.hashCode();
                hash = hash % prm.featureHashMod;
                if (hash < 0) {
                    hash += prm.featureHashMod;
                }
                fname = Integer.toString(hash);
                int fidx = alphabet.lookupIndex(new Feature(fname));
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

        return fv;
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