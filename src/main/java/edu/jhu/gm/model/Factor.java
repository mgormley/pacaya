package edu.jhu.gm.model;

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
     * Adds the expected feature counts for this factor, given the marginal distribution.
     * 
     * @param counts The object collecting the feature counts.
     * @param factorMarginal The marginal distribution for this factor.
     * @param multiplier The multiplier for the added feature accounts.
     */
    void addExpectedFeatureCounts(IFgModel counts, VarTensor factorMarginal, double multiplier);

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