package edu.jhu.gm.feat;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FeExpFamFactor;
import edu.jhu.gm.model.UnsupportedFactorTypeException;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.FeatureNames;

/** Cache of feature vectors for a factor graph. */
public class FeatureCache implements FeatureExtractor {
    
    /** Indexed by factor ID, and the variable configuration. */
    private FeatureVector[][] feats;
    /** The feature extractor to cache. */
    private FeatureExtractor featExtractor;
    
    public FeatureCache(FeatureExtractor featExtractor) {
        this.featExtractor = featExtractor;
    }

    @Override
    public void init(UFgExample ex) {
        this.featExtractor.init(ex);
        FactorGraph fg = ex.getFgLatPred();
        this.feats = new FeatureVector[fg.getNumFactors()][];
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                continue;
            } else if (f instanceof VarTensor) {
                int numConfigs = f.getVars().calcNumConfigs();
                // If there are no latent/predicted variables there must be observed variables.
                numConfigs = (numConfigs == 0) ? 1 : numConfigs;
                feats[a] = new FeatureVector[numConfigs];
            } else {
                throw new UnsupportedFactorTypeException(f);
            }
        }
    }

    /** Gets the feature vector for the specified factor and config. */
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        int factorId = factor.getId();
        if (feats[factorId][configId] == null) {
            feats[factorId][configId] = featExtractor.calcFeatureVector(factor, configId);
        }
        return feats[factorId][configId];
    }

    public String toString(FeatureNames alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < feats.length; a++) {
            for (int c = 0; c < feats[a].length; c++) {
                FeatureVector fv = feats[a][c];
                if (fv != null) {
                    int i=0;
                    for (IntDoubleEntry entry : fv) {
                        if (i++ > 0) {
                            sb.append(", ");
                        }
                        sb.append(alphabet.lookupObject(entry.index()));
                        sb.append("=");
                        sb.append(entry.get());
                    }
                } else {
                    sb.append("null");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
}