package edu.jhu.gm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.Var.VarType;

/**
 * Minimum Bayes Risk (MBR) decoder for a CRF model.
 * 
 * @author mgormley
 */
public class MbrDecoder {

    public static class MbrDecoderPrm {
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public Loss loss = Loss.ACCURACY;
    }

    public enum Loss {
        // TODO: support other loss functions.
        ACCURACY
    }

    private MbrDecoderPrm prm;    
    private ArrayList<VarConfig> mbrVarConfigList;
    private Map<Var,Double> varMargMap;

    public MbrDecoder(MbrDecoderPrm prm) {
        this.prm = prm;
    }

    /**
     * Runs inference and computes the MBR variable configuration and the
     * corresponding features that fire.
     * 
     * @param model The input model.
     * @param data The input data.
     */
    // TODO: we should really pass something other than an FgExamples object in
    // since we might not know the true values of the predicted variables.
    public void decode(FgModel model, FgExamples data) {
        if (prm.loss == Loss.ACCURACY) {
            mbrVarConfigList = new ArrayList<VarConfig>();
            varMargMap = new HashMap<Var,Double>();
            
            for (int i = 0; i < data.size(); i++) {
                FgExample ex = data.get(i);
                VarConfig mbrVarConfig = new VarConfig();

                // Add in the observed variables.
                VarSet obsVars = VarSet.getVarsOfType(ex.getGoldConfig().getVars(), VarType.OBSERVED);
                mbrVarConfig.put(ex.getGoldConfig().getSubset(obsVars));

                // Run inference.
                FactorGraph fgLatPred = ex.getFgLatPred();
                FgInferencer inf = prm.infFactory.getInferencer(fgLatPred);
                fgLatPred = ex.updateFgLatPred(model, inf.isLogDomain());
                inf.run();

                // Get the MBR configuration of all the latent and predicted
                // variables.
                for (int varId = 0; varId < fgLatPred.getNumVars(); varId++) {
                    Var var = fgLatPred.getVar(varId);
                    DenseFactor marg = inf.getMarginalsForVarId(varId);
                    int argmaxState = marg.getArgmaxConfigId();
                    mbrVarConfig.put(var, argmaxState);
                    varMargMap.put(var, marg.getValue(argmaxState));
                }
                
                mbrVarConfigList.add(mbrVarConfig);
            }
        } else {
            throw new RuntimeException("Loss type not implemented: " + prm.loss);
        }
    }
    
    /** Gets the MBR variable configuration for the i'th example. */
    public VarConfig getMbrVarConfig(int i) {
        return mbrVarConfigList.get(i);
    }

    /** Gets a map from the variable to the value of its maximum marginal. */
    public Map<Var, Double> getVarMargMap() {
        return varMargMap;
    }

    public List<VarConfig> getMbrVarConfigList() {
        return mbrVarConfigList;
    }

}
