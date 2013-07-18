package edu.jhu.gm;

import edu.jhu.gm.BeliefPropagation.Messages;
import edu.jhu.gm.FactorGraph.FgNode;

public interface GlobalFactor {

    /**
     * Creates all the messages from this global factor to all its variables.
     * The global factor is responsible for ensuring that it does not do
     * execessive computation for each iteration of BP.
     * 
     * @param parent The node for this global factor.
     * @param msgs The message containers.
     * @param logDomain Whether the resulting messages should be represented in
     *            the log-domain.
     * @param iter The current belief propagation iteration.
     */
    void createMessages(FgNode parent, Messages[] msgs, boolean logDomain, int iter);

}
