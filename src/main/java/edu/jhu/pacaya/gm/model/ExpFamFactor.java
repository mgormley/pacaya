package edu.jhu.pacaya.gm.model;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.erma.AutodiffFactor;
import edu.jhu.pacaya.autodiff.erma.MVecFgModel;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


/**
 * Exponential family factor represented as a dense conditional probability
 * table.
 * 
 * @author mgormley
 */
public abstract class ExpFamFactor extends ExplicitFactor implements Factor, FeatureCarrier, AutodiffFactor {
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ExpFamFactor.class);

    protected boolean initialized = false;

    public ExpFamFactor(VarSet vars) {
        super(vars);
    }
    
    public ExpFamFactor(ExpFamFactor other) {
        super(other);
    }
    
    public ExpFamFactor(VarTensor other) {
        super(other);
    }

    public abstract FeatureVector getFeatures(int config);
        
    /** Gets the unnormalized numerator value contributed by this factor. */
    public double getLogUnormalizedScore(int configId) {
        if (!initialized) {
            throw new IllegalStateException("Factor cannot be queried until updateFromModel() has been called.");
        }
        return super.getLogUnormalizedScore(configId);
    }

    /**
     * If this factor depends on the model, this method will updates this
     * factor's internal representation accordingly.
     * 
     * @param model The model.
     */
    public void updateFromModel(FgModel model) {
        initialized = true;
        int numConfigs = this.getVars().calcNumConfigs();
        for (int c=0; c<numConfigs; c++) {
            double dot = getDotProd(c, model);                
            assert !Double.isNaN(dot) && dot != Double.POSITIVE_INFINITY : "Invalid value for factor: " + dot;
            this.setValue(c, dot);
        }
    }
    
    /**
     * Provide the dot product of the features and model weights for this configuration
     * (make sure to exp that value if !logDomain).
     * 
     * Note that this method can be overridden for an efficient product of an ExpFamFactor
     * and a hard factor (that rules out some configurations). Just return -infinity
     * here before extracting features for configurations that are eliminated by the
     * hard factor. If you do this, the corresponding (by config) implementation of
     * getFeatures should return an empty vector. This is needed because addExpectedFeatureCounts
     * expects to be able to call getFeatures, regardless of whether or not getDotProd has
     * ruled it out as having any mass.
     */
    protected double getDotProd(int config, FgModel model) {
    	 FeatureVector fv = getFeatures(config);
         return model.dot(fv);
    }

    public void addExpectedPartials(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        VarTensor factorMarginal = inferencer.getMarginalsForFactorId(factorId);        
        addExpectedPartials(counts, factorMarginal, multiplier);
    }

    @Override
    public void addExpectedPartials(IFgModel counts, VarTensor factorMarginal, double multiplier) {
        log.trace("factorMarginal = {}", factorMarginal);
        int numConfigs = factorMarginal.getVars().calcNumConfigs();
        for (int c=0; c<numConfigs; c++) {       
            // Get the probability of the c'th configuration for this factor.
            double prob = factorMarginal.getValue(c);

            // Get the feature counts when they are clamped to the c'th configuration for this factor.
            FeatureVector fv = getFeatures(c);

            // Scale the feature counts by the marginal probability of the c'th configuration.
            // Update the gradient for each feature.
            counts.addAfterScaling(fv, multiplier * prob);
        }
    }
        
    @Override
    public Module<VarTensor> getFactorModule(Module<MVecFgModel> modIn, Algebra s) {
        return new ExpFamFactorModule(modIn, s, this);
    }
    
    /**
     * Module for computing exp(\theta \cdot f(x,y)) to populate each of the exponential family factors.
     * 
     * @author mgormley
     */
    public static class ExpFamFactorModule extends AbstractModule<VarTensor> implements Module<VarTensor> {

        private Module<MVecFgModel> modIn;
        private ExpFamFactor f;
        
        public ExpFamFactorModule(Module<MVecFgModel> modIn, Algebra s, ExpFamFactor f) {
            super(s);
            this.modIn = modIn;
            this.f = f;
        }
        
        @Override
        public VarTensor forward() {
            f.updateFromModel(modIn.getOutput().getModel());
            y = BruteForceInferencer.safeNewVarTensor(s, f);
            return y;
        }

        @Override
        public void backward() {
            FgModel modelAdj = modIn.getOutputAdj().getModel();
            VarTensor factorMarginal = new VarTensor(yAdj);
            factorMarginal.prod(y);
            // addExpectedFeatureCounts() currently only supports the real semiring
            assert modIn.getAlgebra().equals(RealAlgebra.getInstance());
            factorMarginal = factorMarginal.copyAndConvertAlgebra(RealAlgebra.getInstance());
            f.addExpectedPartials(modelAdj, factorMarginal, RealAlgebra.getInstance().one());
        }

        @Override
        public List<? extends Module<MVecFgModel>> getInputs() {
            return QLists.getList(modIn);
        }
        
    }
    
}
