package edu.jhu.gm.decode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;

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
        ACCURACY, MSE, L1
    }

    private static final Logger log = Logger.getLogger(MbrDecoder.class);

    private MbrDecoderPrm prm;    
    private VarConfig mbrVarConfig;
    private Map<Var,Double> varMargMap;
    private ArrayList<DenseFactor> margs;

    public MbrDecoder(MbrDecoderPrm prm) {
        this.prm = prm;
    }

    /**
     * Runs inference and computes the MBR variable configuration. The outputs
     * are stored on the class, and can be queried after this call to decode.
     * 
     * @param model The input model.
     * @param ex The input data.
     * @return the FgInferencer that was used.
     */
    public FgInferencer decode(FgModel model, UFgExample ex) {
        // Run inference.
        FactorGraph fgLatPred = ex.updateFgLatPred(model, prm.infFactory.isLogDomain());
        FgInferencer infLatPred = prm.infFactory.getInferencer(fgLatPred);
        infLatPred.run();        
        decode(infLatPred, ex);
        return infLatPred;
    }

    /**
     * Computes the MBR variable configuration from the marginals cached in the
     * inferencer, which is assumed to have already been run. The outputs are
     * stored on the class, and can be queried after this call to decode.
     */
    public void decode(FgInferencer infLatPred, UFgExample ex) {
        FactorGraph fgLatPred = ex.getFgLatPred();
        
        mbrVarConfig = new VarConfig();
        margs = new ArrayList<DenseFactor>();
        varMargMap = new HashMap<Var,Double>();

        // Add in the observed variables.
        mbrVarConfig.put(ex.getObsConfig());

        // Get the MBR configuration of all the latent and predicted
        // variables.        
        if (prm.loss == Loss.ACCURACY || prm.loss == Loss.MSE || prm.loss == Loss.L1) {
            for (int varId = 0; varId < fgLatPred.getNumVars(); varId++) {
                Var var = fgLatPred.getVar(varId);
                DenseFactor marg = infLatPred.getMarginalsForVarId(varId);
                margs.add(marg);
                int argmaxState = marg.getArgmaxConfigId();
                mbrVarConfig.put(var, argmaxState);

                varMargMap.put(var, marg.getValue(argmaxState));
                if (log.isTraceEnabled()) {
                    log.trace("Variable marginal: " + marg);
                }
            }
        } else {
            throw new RuntimeException("Loss type not implemented: " + prm.loss);
        }
    }
    
    /** Gets the MBR variable configuration for the example that was decoded. */
    public VarConfig getMbrVarConfig() {
        return mbrVarConfig;
    }
    
    /** Gets a map from the variable to the value of its maximum marginal. */
    public Map<Var, Double> getVarMargMap() {
        return varMargMap;
    }

    /**
     * Gets the marginal distribution for each variable in this factor graph.
     * The i'th DenseFactor in the list corresponds to the i'th variable in the
     * factor graph.
     */
    public List<DenseFactor> getVarMarginals() {
        return margs;
    }

    /**
     * Convenience wrapper around getVarMarginals().
     * Does not memoize, so use sparingly.
     */
    public Map<Var, DenseFactor> getVarMarginalsIndexed() {
    	Map<Var, DenseFactor> m = new HashMap<Var, DenseFactor>();
    	for(DenseFactor df : margs) {
    		assert df.getVars().size() == 1;
    		m.put(df.getVars().get(0), df);
    	}
    	return m;
    }
}
