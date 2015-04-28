package edu.jhu.pacaya.gm.model.globalfac;

import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.IFgModel;
import edu.jhu.pacaya.gm.model.VarTensor;

/**
 * A structured factor that permits efficient computation of its outgoing messages.
 * 
 * @author mgormley
 */
public interface GlobalFactor extends Factor {

    /**
     * Computes and sets the messages from this global factor to each of its variables.
     * 
     * @param inMsgs The incoming messages to this factor.
     * @param outMsgs The outgoing messages from this factor. (OUTPUT)
     */
    void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs);

    /**
     * Gets the expected log beliefs for this factor. We include factor's potential function in the
     * expectation since for most constraint factors \chi(x_a) \in \{0,1\}.
     * <p>
     * E[ln(b(x_a) / \chi(x_a)) ] = \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
     * <p>
     * Note: The value should be returned as a real, though the messages may be in a different
     * semiring.
     * 
     * @param inMsgs The incoming messages to this factor.
     * @return The expected log belief.
     */
    double getExpectedLogBelief(VarTensor[] inMsgs);
    
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

}
