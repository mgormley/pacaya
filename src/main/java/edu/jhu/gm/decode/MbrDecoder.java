package edu.jhu.gm.decode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.FgInferencerFactory;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.Prm;

/**
 * Minimum Bayes Risk (MBR) decoder for a CRF model.
 * 
 * @author mgormley
 */
public class MbrDecoder implements Decoder<Object, VarConfig> {

    public static class MbrDecoderPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public FgInferencerFactory infFactory = new BeliefPropagationPrm();
        public Loss loss = Loss.L1;
    }

    public enum Loss {
        L1, MSE
    }

    private static final Logger log = LoggerFactory.getLogger(MbrDecoder.class);

    private MbrDecoderPrm prm;    
    private VarConfig mbrVarConfig;
    private Map<Var,Double> varMargMap;
    private ArrayList<VarTensor> margs;

    public MbrDecoder(MbrDecoderPrm prm) {
        this.prm = prm;
    }

    @Override
    public VarConfig decode(FgInferencer inf, UFgExample ex, Object x) {
        decode(inf, ex);
        return mbrVarConfig;
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
        FactorGraph fgLatPred = ex.getFgLatPred();
        FgInferencer infLatPred = prm.infFactory.getInferencer(fgLatPred, model);
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
        margs = new ArrayList<VarTensor>();
        varMargMap = new HashMap<Var,Double>();

        // Add in the observed variables.
        mbrVarConfig.put(ex.getObsConfig());

        // Get the MBR configuration of all the latent and predicted
        // variables.        
        if (prm.loss == Loss.L1 || prm.loss == Loss.MSE) {
            for (int varId = 0; varId < fgLatPred.getNumVars(); varId++) {
                Var var = fgLatPred.getVar(varId);
                VarTensor marg = infLatPred.getMarginalsForVarId(varId);
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
    public List<VarTensor> getVarMarginals() {
        return margs;
    }

    /**
     * Convenience wrapper around getVarMarginals().
     * Does not memoize, so use sparingly.
     */
    public Map<Var, VarTensor> getVarMarginalsIndexed() {
    	Map<Var, VarTensor> m = new HashMap<Var, VarTensor>();
    	for(VarTensor df : margs) {
    		assert df.getVars().size() == 1;
    		m.put(df.getVars().get(0), df);
    	}
    	return m;
    }
    
}
