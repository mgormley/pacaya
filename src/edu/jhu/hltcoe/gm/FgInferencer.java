package edu.jhu.hltcoe.gm;

import edu.jhu.hltcoe.gm.FactorGraph.Factor;
import edu.jhu.hltcoe.gm.FactorGraph.Var;
import edu.jhu.hltcoe.gm.FactorGraph.VarSet;
import edu.jhu.hltcoe.util.vector.SortedIntDoubleVector;

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
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single variable.
     */
    Factor getMarginals(Var var);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * set of variables.
     */
    Factor getMarginals(VarSet varSet);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single factor.
     */
    Factor getMarginals(Factor factor);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single variable specified by its index.
     */
    Factor getMarginalsForVarId(int varId);

    /**
     * Gets the approximate log-probabilities for the marginal distribution of a
     * single factor specified by its index.
     */
    Factor getMarginalsForFactorId(int factorId);

    /**
     * Gets the log of the (approximate) partition function, which is the
     * normalizing constant for the factor graph.
     */
    double getLogPartition(FgExample ex, FgModel model);
    
}
