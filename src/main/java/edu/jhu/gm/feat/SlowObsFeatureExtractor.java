package edu.jhu.gm.feat;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeExpFamFactor;
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
    protected VarConfig goldConfig;
    protected FactorTemplateList fts;

    public SlowObsFeatureExtractor() {    }
    
    @Override
    public void init(FgExample ex, FactorTemplateList fts) {
        this.fg = ex.getOriginalFactorGraph();
        this.goldConfig = ex.getGoldConfig();
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor) {
        VarSet vars = factor.getVars();
        return calcObsFeatureVector(factor, goldConfig.getSubset(VarSet.getVarsOfType(vars, VarType.OBSERVED)));
    }
    
    public abstract FeatureVector calcObsFeatureVector(ObsFeExpFamFactor factor, VarConfig varConfig);
}
