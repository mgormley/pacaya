package edu.jhu.gm.feat;

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
    
    public void init(FactorGraph fg, FactorGraph fgLat, FactorGraph fgLatPred, VarConfig goldConfig, FactorTemplateList fts) {
        this.fg = fg;
        this.goldConfig = goldConfig;
        this.fts = fts;
    }
    
    @Override
    public FeatureVector calcObsFeatureVector(int factorId) {
        VarSet vars = fg.getFactor(factorId).getVars();
        return calcObsFeatureVector(factorId, goldConfig.getSubset(VarSet.getVarsOfType(vars, VarType.OBSERVED)));
    }
    
    @Override
    public void clear() {
        // Do nothing.
    }
    
    public abstract FeatureVector calcObsFeatureVector(int factorId, VarConfig varConfig);
}
