package edu.jhu.gm;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/** Cache of feature vectors for a factor graph. */
public class ObsFeatureCache implements ObsFeatureExtractor {
    
    /** Indexed by factor ID. */
    private FeatureVector[] feats;
    /** The feature extractor to cache. */
    private ObsFeatureExtractor featExtractor;
    
    public ObsFeatureCache(FactorGraph fg, ObsFeatureExtractor featExtractor) {
        this.feats = new FeatureVector[fg.getNumFactors()];
        this.featExtractor = featExtractor;
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                continue;
            } else if (f instanceof DenseFactor) {
                continue;
            } else {
                throw new UnsupportedFactorTypeException(f);
            }
        }
    }

    /** Gets the feature vector for the specified factor and config. */
    public FeatureVector calcObsFeatureVector(int factorId) {
        if (feats[factorId] == null) {
            feats[factorId] = featExtractor.calcObsFeatureVector(factorId);
        }
        return feats[factorId];
    }

    public String toString(Alphabet<Feature> alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < feats.length; a++) {
            FeatureVector fv = calcObsFeatureVector(a);
            int i=0;
            for (IntDoubleEntry entry : fv) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(alphabet.lookupObject(entry.index()));
                sb.append("=");
                sb.append(entry.get());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
}