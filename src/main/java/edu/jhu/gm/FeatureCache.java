package edu.jhu.gm;

/** Cache of feature vectors for a factor graph. */
public class FeatureCache {
    
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
    
}