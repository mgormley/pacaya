package edu.jhu.gm;

import edu.jhu.gm.Var.VarType;

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
    public FeatureVector calcFeatureVector(int factorId, int configId) {
        VarSet vars = fg.getFactor(factorId).getVars();
        VarSet latPredVars = new VarSet(VarSet.getVarsOfType(vars, VarType.PREDICTED), VarSet.getVarsOfType(vars, VarType.LATENT));
        VarConfig varConfig = latPredVars.getVarConfig(configId);
        varConfig = new VarConfig(goldConfig.getSubset(VarSet.getVarsOfType(vars, VarType.OBSERVED)), varConfig);
        return calcFeatureVector(factorId, varConfig);
    }
    
    public abstract FeatureVector calcFeatureVector(int factorId, VarConfig varConfig);
}
