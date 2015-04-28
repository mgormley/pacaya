package edu.jhu.pacaya.gm.feat;

import edu.jhu.pacaya.gm.data.UFgExample;
import edu.jhu.pacaya.gm.model.FeExpFamFactor;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.Var.VarType;

/**
 * For testing only.
 * 
 * Gives (convenient) access to the VarConfig of the factor.
 * 
 * @author mgormley
 */
public abstract class SlowFeatureExtractor implements FeatureExtractor {

    public VarConfig obsConfig;

    public SlowFeatureExtractor() {
    }

    @Override
    public void init(UFgExample ex) {
        this.obsConfig = ex.getObsConfig();
    }
    
    @Override
    public FeatureVector calcFeatureVector(FeExpFamFactor factor, int configId) {
        VarSet vars = factor.getVars();
        VarSet latPredVars = new VarSet(VarSet.getVarsOfType(vars, VarType.PREDICTED), VarSet.getVarsOfType(vars, VarType.LATENT));
        VarConfig varConfig = latPredVars.getVarConfig(configId);
        varConfig = new VarConfig(obsConfig, varConfig);
        return calcFeatureVector(factor, varConfig);
    }
    
    public abstract FeatureVector calcFeatureVector(FeExpFamFactor factor, VarConfig varConfig);
}
