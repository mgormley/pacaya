package edu.jhu.pacaya.gm.model;

import java.io.Serializable;


public interface Factor extends Serializable {
    
    /**
     * Gets a new version of the factor graph where the specified variables are
     * clamped to their given values.
     */
    Factor getClamped(VarConfig clmpVarConfig);

    /** Gets the variables associated with this factor. */
    VarSet getVars();

    /**
     * If this factor depends on the model, this method will updates this
     * factor's internal representation accordingly.
     */
    void updateFromModel(FgModel model);
    
    /** Gets the unnormalized numerator value contributed by this factor. */
    double getLogUnormalizedScore(VarConfig goldConfig);
    
    /** Gets the unnormalized numerator value contributed by this factor. */
    double getLogUnormalizedScore(int goldConfig);

    /**
     * Adds the expected partials for this factor, given the marginal distribution.
     * This corresponds to the following expectation:
     * 
     * <pre>
     * d/d\theta_i log p(y) 
     *     += \sum_{y_{\alpha}} p(y_{\alpha}) d/d\theta_i \log \psi_{\alpha}(y_{\alpha})
     * </pre>
     * 
     * If the factor is in the exponential family, this is equivalent to adding the expected feature
     * counts. This falls out of the partial derivative as below:
     * 
     * <pre>
     * d/d\theta_i \log \psi_{\alpha}(y_{\alpha})
     *     = d/d\theta_i (\theta \cdot f(y_{\alpha}), x)) 
     *     = f_i(y_{\alpha}, x)
     * </pre>
     * 
     * @param counts The accumulator for the partial derivatives.
     * @param factorMarginal The marginal distribution for this factor.
     * @param multiplier The multiplier for the added partials.
     */
    void addExpectedPartials(IFgModel counts, VarTensor factorMarginal, double multiplier);

    //    /**
    //     * Whether this is a global factor (i.e. the states of this factor are not enumerable given
    //     * reasonable space requirements).
    //     */
    //    boolean isGlobal();
    
    /** Gets the ID of this factor relative to its factor graph. */
    int getId();

    /**
     * Sets the ID of this factor relative to its factor graph. (This is usually only called by the
     * factor graph when the factor is added.)
     */
    void setId(int id);    
    
}