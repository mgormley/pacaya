package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.util.IncrIntIter;
import edu.jhu.gm.util.IntIter;
import edu.jhu.prim.util.math.FastMath;


/**
 * Exponential family factor represented as a dense conditional probability
 * table.
 * 
 * @author mgormley
 */
public abstract class ExpFamFactor extends ExplicitFactor implements Factor, FeatureCarrier {
    
    private static final long serialVersionUID = 1L;
        
    // The following two fields are stored in order to enable special case logic for 
    // clamped factors which represent the numerator factor graph.
    protected IntIter iter;
    protected int clmpConfigId = -1;

    public ExpFamFactor(VarSet vars) {
        super(vars);
        this.iter = new IncrIntIter(getVars().calcNumConfigs());
    }
    
    public ExpFamFactor(ExpFamFactor other) {
        super(other);
        this.iter = new IncrIntIter(getVars().calcNumConfigs());
    }
    
    public ExpFamFactor(DenseFactor other) {
        super(other);
        this.iter = new IncrIntIter(getVars().calcNumConfigs());
    }

    public abstract FeatureVector getFeatures(int config);
        
    /** Gets the unnormalized numerator value contributed by this factor. */
    public double getUnormalizedScore(int configId) {
        return super.getUnormalizedScore(configId);
    }

    /**
     * If this factor depends on the model, this method will updates this
     * factor's internal representation accordingly.
     * 
     * @param model The model.
     * @param logDomain Whether to store values in the probability or
     *            log-probability domain.
     */
    public void updateFromModel(FgModel model, boolean logDomain) {
        if (iter != null) { iter.reset(); }
        
        ExpFamFactor f = this;
        int numConfigs = f.getVars().calcNumConfigs();
        if (numConfigs > 0) {            
            for (int c=0; c<numConfigs; c++) {
    
                // The configuration of all the latent/predicted variables,
                // where the predicted variables (might) have been clamped.
                int config = (iter != null) ? iter.next() : c;
                
                double dot = getDotProd(config, model, logDomain);
                f.setValue(c, dot);
            }
            assert(iter == null || !iter.hasNext());
        }
    }
    
    /**
     * Provide the dot product of the features and model weights for this configuration
     * (make sure to exp that value if !logDomain).
     * 
     * Note that this method can be overridden for an efficient product of an ExpFamFactor
     * and a hard factor (that rules out some configurations). Just return 0/-infinity
     * here before extracting features for configurations that are eliminated by the
     * hard factor. 
     */
    public double getDotProd(int config, FgModel model, boolean logDomain) {
    	 FeatureVector fv = getFeatures(config);
         double dot = model.dot(fv);
         if (logDomain) return dot;
         else return FastMath.exp(dot);
    }

    public void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        ExpFamFactor f = this;

        DenseFactor factorMarginal = inferencer.getMarginalsForFactorId(factorId);
        
        int numConfigs = factorMarginal.getVars().calcNumConfigs();
        if (numConfigs == 0) {
            // If there are no variables in this factor, we still need to get the cached features.
            // Update the gradient for each feature.
            FeatureVector fv = getFeatures(clmpConfigId);
            counts.addAfterScaling(fv, multiplier);
        } else {
            if (iter != null) { iter.reset(); }

            for (int c=0; c<numConfigs; c++) {       
                // Get the probability of the c'th configuration for this factor.
                double prob = factorMarginal.getValue(c);
                if (inferencer.isLogDomain()) {
                    prob = FastMath.exp(prob);
                }
                // The configuration of all the latent/predicted variables,
                // where the predicted variables (might) have been clamped.
                int config = (iter != null) ? iter.next() : c;

                // Get the feature counts when they are clamped to the c'th configuration for this factor.
                FeatureVector fv = getFeatures(config);

                // Scale the feature counts by the marginal probability of the c'th configuration.
                // Update the gradient for each feature.
                counts.addAfterScaling(fv, multiplier * prob);
            }
            assert(iter == null || !iter.hasNext());
        }
    }

    public ExpFamFactor getClamped(VarConfig clmpVarConfig) {
        DenseFactor df = super.getClamped(clmpVarConfig);
        return new ClampedExpFamFactor(df, clmpVarConfig, this);
    }
    
    protected static class ClampedExpFamFactor extends ExpFamFactor {
        
        private static final long serialVersionUID = 1L;
		
		// The unclamped factor from which this one was derived
        private ExpFamFactor unclmpFactor;
        
        // Used only to create clamped factors.
        public ClampedExpFamFactor(DenseFactor other, VarConfig clmpVarConfig, ExpFamFactor unclmpFactor) {
            super(other);
            this.unclmpFactor = unclmpFactor;
            VarSet unclmpVarSet = unclmpFactor.getVars();
            if (VarSet.getVarsOfType(unclmpVarSet, VarType.OBSERVED).size() == 0) {
                // Only store the unclampedVarSet if it does not contain OBSERVED variables.
                // This corresponds to only storing the VarSet if this is a factor graph 
                // containing only latent variables.
                //
                // TODO: Switch this to an option.
                //
                // If this is the numerator then we must clamp the predicted
                // variables to determine the correct set of model
                // parameters.
                iter = IndexForVc.getConfigIter(unclmpVarSet, clmpVarConfig);
                clmpConfigId = clmpVarConfig.getConfigIndex();
            }
        }

        @Override
        public FeatureVector getFeatures(int config) {
            // Pass through to the unclamped factor.
            return unclmpFactor.getFeatures(config);
        }
        
    }
}
