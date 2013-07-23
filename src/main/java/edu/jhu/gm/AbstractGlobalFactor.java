package edu.jhu.gm;

import edu.jhu.gm.BeliefPropagation.Messages;
import edu.jhu.gm.FactorGraph.FgNode;

public abstract class AbstractGlobalFactor implements GlobalFactor {

    private int iterAtLastCreateMessagesCall = -1;

    public AbstractGlobalFactor() {
        reset();
    }
    
    
    @Override
    public void createMessages(FgNode parent, Messages[] msgs, boolean logDomain, int iter) {
        if (iterAtLastCreateMessagesCall < iter) {
            createMessages(parent, msgs, logDomain);            
            iterAtLastCreateMessagesCall = iter;
        }
    }

    @Override
    public void reset() {
        iterAtLastCreateMessagesCall = -1;
    }
    
    protected abstract void createMessages(FgNode parent, Messages[] msgs, boolean logDomain);

}
