package edu.jhu.gm.model.globalfac;

import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.util.semiring.Algebra;

/**
 * A constraint global factor.
 * 
 * Unlike a full global factor, this does not have any parameters or features.
 * 
 * @author mgormley
 */
public interface GlobalFactor extends Factor {

    /**
     * Creates all the messages from this global factor to all its variables.
     * The global factor is responsible for ensuring that it does not do
     * excessive computation for each iteration of BP, so the factor may choose for 
     * this to be a "no-op" as indicated by its return value.
     * 
     * @param parent The node for this global factor.
     * @param msgs The message containers.
     * @param logDomain Whether the resulting messages should be represented in
     *            the log-domain.
     * @param normalizeMessages TODO
     * @param iter The current belief propagation iteration.
     * @return Whether the messages were created.
     */
    boolean createMessages(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages, int iter);

    /**
     * Resets this global factor for a new run of belief propagation.
     */
    void reset();

    /**
     * Gets the expected log beliefs for this factor. We include factor's
     * potential function in the expectation since for most constraint factors
     * \chi(x_a) \in \{0,1\}.
     * 
     * E[ln(b(x_a) / \chi(x_a)) ] = \sum_{x_a} b(x_a) ln (b(x_a) / \chi(x_a))
     */
    double getExpectedLogBelief(FgNode parent, Messages[] msgs, boolean logDomain);
    
    /**
     * Computes all the message adjoints. This method will only be called once
     * per iteration (unlike createMessages).
     * 
     * @param parent The node for this global factor.
     * @param msgs The messages.
     * @param msgsAdj The adjoints of the messages.
     * @param s The abstract algebra in which to represent the adjoints.
     */
    void backwardCreateMessages(FgNode parent, Messages[] msgs, Messages[] msgsAdj, Algebra s);

}
