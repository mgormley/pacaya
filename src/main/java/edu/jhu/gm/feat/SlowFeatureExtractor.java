package edu.jhu.gm.feat;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FeExpFamFactor;
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
public abstract class SlowFeatureExtractor implements FeatureExtractor {

    private FactorGraph fg;
    private VarConfig goldConfig;

    public SlowFeatureExtractor(FactorGraph fg, VarConfig goldConfig) {
        this.fg = fg;
        this.goldConfig = goldConfig;
    }
    
    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        VarSet vars = factor.getVars();
        VarSet latPredVars = new VarSet(VarSet.getVarsOfType(vars, VarType.PREDICTED), VarSet.getVarsOfType(vars, VarType.LATENT));
        VarConfig varConfig = latPredVars.getVarConfig(configId);
        varConfig = new VarConfig(goldConfig.getSubset(VarSet.getVarsOfType(vars, VarType.OBSERVED)), varConfig);
        return calcFeatureVector(factor, varConfig);
    }
    
    public abstract FeatureVector calcFeatureVector(FeExpFamFactor factor, VarConfig varConfig);
}
