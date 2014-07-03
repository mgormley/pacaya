package edu.jhu.gm.feat;

import edu.jhu.gm.data.UFgExample;
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

    private VarConfig obsConfig;

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
