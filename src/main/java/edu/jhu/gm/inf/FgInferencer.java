package edu.jhu.gm.inf;

import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;

/**
 * An inference algorithm for a factor graph.
 * 
 * @author mgormley
 * 
 */
public interface FgInferencer {

    /**
     * Runs the approximate inference algorithm. This must always be called
     * before getting the beliefs or log-partition function values.
     */
    void run();

    /**
     * Gets the approximate (log-)probabilities for the marginal distribution of
     * a single variable.
     * 
     * @return The marginal distribution as log-probabilities if isLogDomain()
     *         == true, and probabilities otherwise.
     */
    DenseFactor getMarginals(Var var);

    /**
     * Gets the approximate (log-)probabilities for the marginal distribution of
     * a set of variables.
     * 
     * @return The marginal distribution as log-probabilities if isLogDomain()
     *         == true, and probabilities otherwise.
     */
    DenseFactor getMarginals(VarSet varSet);

    /**
     * Gets the approximate (log-)probabilities for the marginal distribution of
     * a single factor.
     * 
     * @return The marginal distribution as log-probabilities if isLogDomain()
     *         == true, and probabilities otherwise.
     */
    DenseFactor getMarginals(Factor factor);

    /**
     * Gets the approximate (log-)probabilities for the marginal distribution of
     * a single variable specified by its index.
     * 
     * @return The marginal distribution as log-probabilities if isLogDomain()
     *         == true, and probabilities otherwise.
     */
    DenseFactor getMarginalsForVarId(int varId);

    /**
     * Gets the approximate (log-)probabilities for the marginal distribution of
     * a single factor specified by its index.
     * 
     * @return The marginal distribution as log-probabilities if isLogDomain()
     *         == true, and probabilities otherwise.
     */
    DenseFactor getMarginalsForFactorId(int factorId);

    /**
     * Gets the log of the (approximate) (log-)partition function, which is the
     * normalizing constant for the factor graph.
     * 
     * @return The log-partition function if isLogDomain() == true, the
     *         partition function otherwise.
     */
    double getPartition();

    /**
     * Whether the inferencer returns marginals and the partition function in
     * the log-domain or the probability domain.
     */
    boolean isLogDomain();

    /** Clear any unneeded state to save memory. */
    void clear();

}
