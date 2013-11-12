package edu.jhu.gm.feat;

import java.io.Serializable;

import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.GlobalFactor;
import edu.jhu.gm.model.UnsupportedFactorTypeException;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/** Cache of feature vectors for a factor graph. */
public class ObsFeatureCache implements ObsFeatureExtractor, Serializable {
    
    private static final long serialVersionUID = 1L;
    /** Indexed by factor ID. */
    private FeatureVector[] feats;
    /** The feature extractor to cache. */
    private ObsFeatureExtractor featExtractor;
    
    public ObsFeatureCache(FactorGraph fg, ObsFeatureExtractor featExtractor) {
        this.featExtractor = featExtractor;
        this.feats = new FeatureVector[fg.getNumFactors()];
    }

    @Override
    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig,
            FeatureTemplateList fts) {
        this.featExtractor.init(fg, fgLat, fgLatPred, goldConfig, fts);
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                continue;
            } else if (f instanceof ExpFamFactor) {
                feats[a] = featExtractor.calcObsFeatureVector(a);
                // TODO: maybe call calcObsFeatureVector(a); and don't store featExtractor.
                continue;
            } else {
                throw new UnsupportedFactorTypeException(f);
            }
        }
        this.featExtractor = null;
    }

    /** Gets the feature vector for the specified factor and config. */
    public FeatureVector calcObsFeatureVector(int factorId) {
        if (feats[factorId] == null) {
            throw new IllegalStateException();// feats[factorId] = featExtractor.calcObsFeatureVector(factorId);
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

    @Override
    public void clear() {
        featExtractor = null;
    }
    
}