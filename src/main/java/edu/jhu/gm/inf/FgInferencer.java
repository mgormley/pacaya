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
     * Gets the approximate probabilities for the marginal distribution of a
     * single variable.
     * 
     * @return The marginal distribution as probabilities
     */
    DenseFactor getMarginals(Var var);

    /**
     * Gets the approximate probabilities for the marginal distribution of a
     * single factor.
     * 
     * @return The marginal distribution as probabilities
     */
    DenseFactor getMarginals(Factor factor);

    /**
     * Gets the approximate probabilities for the marginal distribution of a
     * single variable specified by its index.
     * 
     * @return The marginal distribution as probabilities
     */
    DenseFactor getMarginalsForVarId(int varId);

    /**
     * Gets the approximate probabilities for the marginal distribution of a
     * single factor specified by its index.
     * 
     * @return The marginal distribution as probabilities
     */
    DenseFactor getMarginalsForFactorId(int factorId);

    /**
     * Gets the (approximate) partition function, which is the normalizing
     * constant for the factor graph.
     * 
     * @return The partition function.
     */
    double getPartition();

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single variable.
     * 
     * @return The marginal distribution as log-probabilities.
     */
    DenseFactor getLogMarginals(Var var);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single factor.
     * 
     * @return The marginal distribution as log-probabilities.
     */
    DenseFactor getLogMarginals(Factor factor);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single variable specified by its index.
     * 
     * @return The marginal distribution as log-probabilities.
     */
    DenseFactor getLogMarginalsForVarId(int varId);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single factor specified by its index.
     * 
     * @return The marginal distribution as log-probabilities.
     */
    DenseFactor getLogMarginalsForFactorId(int factorId);

    /**
     * Gets the log of the (approximate) partition function, which is the
     * normalizing constant for the factor graph.
     * 
     * @return The log-partition function.
     */
    double getLogPartition();

    /**
     * Whether the inferencer treats the factors as containing the log of their
     * values or not.
     */
    boolean isLogDomain();

}
