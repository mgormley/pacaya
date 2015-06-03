package edu.jhu.pacaya.gm.feat;

import java.io.Serializable;

import edu.jhu.pacaya.gm.data.UFgExample;
import edu.jhu.pacaya.util.FeatureNames;
import edu.jhu.prim.map.IntDoubleEntry;

/** Cache of feature vectors for a factor graph. */
public class ObsFeatureCache implements ObsFeatureExtractor, Serializable {
    
    private static final long serialVersionUID = 1L;
    /** Indexed by factor ID. */
    private FeatureVector[] feats;
    /** The feature extractor to cache. */
    private ObsFeatureExtractor featExtractor;
    
    public ObsFeatureCache(ObsFeatureExtractor featExtractor) {
        this.featExtractor = featExtractor;
    }

    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        this.feats = new FeatureVector[ex.getFgLatPred().getNumFactors()];
        this.featExtractor.init(ex, fts);
    }

    /** Gets the feature vector for the specified factor and config. */
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        int factorId = factor.getId();
        if (feats[factorId] == null) {
            feats[factorId] = featExtractor.calcObsFeatureVector(factor);
        }
        return feats[factorId];
    }

    public String toString(FeatureNames alphabet) {
        StringBuilder sb = new StringBuilder();
        for (int a = 0; a < feats.length; a++) {
            FeatureVector fv = feats[a];
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
        return sb.toString();
    }
    
}