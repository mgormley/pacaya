package edu.jhu.gm.model;

import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.FactorGraph.FgNode;

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
     * execessive computation for each iteration of BP.
     * 
     * @param parent The node for this global factor.
     * @param msgs The message containers.
     * @param logDomain Whether the resulting messages should be represented in
     *            the log-domain.
     * @param normalizeMessages TODO
     * @param iter The current belief propagation iteration.
     */
    void createMessages(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages, int iter);

    /**
     * Resets this global factor for a new run of belief propagation.
     */
    void reset();

}
