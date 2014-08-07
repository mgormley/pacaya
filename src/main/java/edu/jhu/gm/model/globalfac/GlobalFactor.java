package edu.jhu.gm.model.globalfac;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.Messages;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarTensor;

/**
 * A constraint global factor.
 * 
 * Unlike a full global factor, this does not have any parameters or features.
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
     * 
     * E[ln(b(x_a) / \chi(x_a)) ] = \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
     * 
     * Note: The value should be returned as a real, though the messages may be in a different
     * semiring.
     * 
     * @param inMsgs The incoming messages to this factor.
     * @return The expected log belief.
     */
    double getExpectedLogBelief(VarTensor[] inMsgs);
    
    /**
     * Computes and sets the adjoints of the incoming messages.
     * 
     * @param inMsgs The incoming messages to this factor.
     * @param outMsgsAdj The adjoints of the outgoing messages from this factor.
     * @param inMsgs The adjoints of the incoming messages to this factor. (OUTPUT)
     */
    void backwardCreateMessages(VarTensor[] inMsgs, VarTensor[] outMsgsAdj, VarTensor[] inMsgsAdj);
    
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
