package edu.jhu.gm;

import edu.jhu.util.Alphabet;
import edu.jhu.util.vector.IntDoubleEntry;

/** Cache of feature vectors for a factor graph. */
public class FeatureCache {
    
    /** Indexed by factor ID, and the variable configuration. */
    private FeatureVector[][] feats;
    
    public FeatureCache(FactorGraph fg) {
        feats = new FeatureVector[fg.getNumFactors()][];
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            int numConfigs = f.getVars().calcNumConfigs();
            if (numConfigs == 0) {
                feats[a] = new FeatureVector[1];
            } else {
                feats[a] = new FeatureVector[numConfigs];
            }
        }
    }

    /** Gets the feature vector for the specified factor and config. */
    public FeatureVector getFeatureVector(int factorId, int configId) {
        return feats[factorId][configId];
    }

    /** Sets the feature vector for the specified factor and config. */
    public void setFv(int factorId, int configId, FeatureVector fv) {
        feats[factorId][configId] = fv;            
    }

    public String toString(Alphabet<Feature> alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < feats.length; a++) {
            for (int c = 0; c < feats[a].length; c++) {
                FeatureVector fv = getFeatureVector(a, c);
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
            sb.append("\n");
        }
        return sb.toString();
    }
    
}