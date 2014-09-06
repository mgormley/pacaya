package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.featurize.LocalObservations;
import edu.jhu.featurize.TemplateFeatureExtractor;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.feat.ObsFeExpFamFactor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelVar;
import edu.jhu.nlp.relations.RelationsFactorGraphBuilder.RelationsFactorGraphBuilderPrm;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Alphabet;
import edu.jhu.util.hash.MurmurHash3;

/**
 * Feature extraction for relations.
 * @author mgormley
 */
// TODO: This replicates a lot of code in SrlFeatureExtractor. 
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
        addFeatures(biasFeats, alphabet, fv, true, prm.featureHashMod);
        
        // Add the other features.
        addFeatures(obsFeats, alphabet, fv, false, prm.featureHashMod);
        
        return fv;
    } 
    
    /**
     * Adds each feature to fv using the given alphabet.
     */
    public static void addFeatures(Collection<String> obsFeats, Alphabet<Feature> alphabet, FeatureVector fv, boolean isBiasFeat, int featureHashMod) {
        if (featureHashMod <= 0) {
            // Just use the features as-is.
            for (String fname : obsFeats) {
                int fidx = alphabet.lookupIndex(new Feature(fname, isBiasFeat));
                if (fidx != -1) {
                    fv.add(fidx, 1.0);
                }
            }
        } else {
            // Apply the feature-hashing trick.
            for (String fname : obsFeats) {
                int hash = MurmurHash3.murmurhash3_x86_32(fname);
                hash = FastMath.mod(hash, featureHashMod);
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
     * Returns the hash code of the reverse of this string.
     */
    public static int reverseHashCode(String fname) {
        int hash = 0;
        int n = fname.length();
        for (int i=n-1; i>=0; i--) {
            hash += 31 * hash + fname.charAt(i);
        }
        return hash;
    }

    
}