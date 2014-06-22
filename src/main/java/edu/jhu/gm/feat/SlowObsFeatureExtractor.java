package edu.jhu.gm.feat;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;

/**
 * For testing only.
 * 
 * Gives (convenient) access to the VarConfig of the factor.
 * 
 * @author mgormley
 */
public abstract class SlowObsFeatureExtractor implements ObsFeatureExtractor {

    protected FactorGraph fg;
    protected VarConfig obsConfig;
    protected FactorTemplateList fts;

    public SlowObsFeatureExtractor() {    }
    
    @Override
    public void init(UFgExample ex, FactorTemplateList fts) {
        this.fg = ex.getOriginalFactorGraph();
        this.obsConfig = ex.getObsConfig();
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        return calcObsFeatureVector(factor, obsConfig);
    }
    
    public abstract FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor, VarConfig varConfig);
}
