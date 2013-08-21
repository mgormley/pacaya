package edu.jhu.gm;

import edu.jhu.gm.Var.VarType;

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
    protected FeatureTemplateList fts;

    public SlowObsFeatureExtractor() {    }
    
    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig, FeatureTemplateList fts) {
        this.fg = fg;
        this.goldConfig = goldConfig;
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(int factorId) {
        VarSet vars = fg.getFactor(factorId).getVars();
        return calcObsFeatureVector(factorId, goldConfig.getSubset(VarSet.getVarsOfType(vars, VarType.OBSERVED)));
    }
    
    public abstract FeatureVector calcObsFeatureVector(int factorId, VarConfig varConfig);
}
