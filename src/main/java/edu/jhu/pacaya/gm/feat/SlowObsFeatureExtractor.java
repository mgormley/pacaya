package edu.jhu.pacaya.gm.feat;

import edu.jhu.pacaya.gm.data.UFgExample;
import edu.jhu.pacaya.gm.model.FactorGraph;

/**
 * For testing only.
 * 
 * Gives (convenient) access to the VarConfig of the factor.
 * 
 * @author mgormley
 */
public abstract class SlowObsFeatureExtractor implements ObsFeatureExtractor {

    protected FactorGraph fg;
    protected FactorTemplateList fts;

    public SlowObsFeatureExtractor() {    }
    
    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        this.fg = ex.getFactorGraph();
        this.fts = fts;
    }
    
    public abstract FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor);
}
