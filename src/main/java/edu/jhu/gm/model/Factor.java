package edu.jhu.gm.model;

import java.io.Serializable;

import edu.jhu.gm.inf.FgInferencer;


public interface Factor extends Serializable {
    
    /**
     * Gets a new version of the factor graph where the specified variables are
     * clamped to their given values.
     */
    Factor getClamped(VarConfig clmpVarConfig);

    /** Gets the variables associated with this factor. */
    VarSet getVars();

    /**
     * If this factor depends on the model, this method wil updates this
     * factor's internal representation accordingly.
     * 
     * @param model The model.
     * @param logDomain Whether to store values in the probability or
     *            log-probability domain.
     */
    void updateFromModel(FgModel model, boolean logDomain);

    /** Gets the unnormalized numerator value contributed by this factor. */
    double getUnormalizedScore(int configId);

    /**
     * Adds the expected feature counts for this factor, given the marginal distribution 
     * specified by the inferencer for this factor.
     * 
     * @param counts The object collecting the feature counts.
     * @param multiplier The multiplier for the added feature accounts.
     * @param inferencer The inferencer from which the marginal distribution is taken.
     * @param factorId The id of this factor within the inferencer.
     */
    void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId);

    int getId();
    void setId(int id);    
    
}