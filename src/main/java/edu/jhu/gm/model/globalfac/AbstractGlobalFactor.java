package edu.jhu.gm.model.globalfac;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.Messages;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.FactorGraph.FgNode;

public abstract class AbstractGlobalFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
    private int id = -1;
    private int iterAtLastCreateMessagesCall = -1;
    protected boolean logDomain;
    
    public AbstractGlobalFactor() {
        reset();
    }
        
    @Override
    public boolean createMessages(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages, int iter) {
        if (iterAtLastCreateMessagesCall < iter) {
            createMessages(parent, msgs, logDomain);            
            iterAtLastCreateMessagesCall = iter;
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
        iterAtLastCreateMessagesCall = -1;
    }

    public void updateFromModel(FgModel model, boolean logDomain) {
        // Currently, global factors do not support features, and
        // therefore have no model parameters.
        this.logDomain = logDomain;
    }   

    public void addExpectedFeatureCounts(IFgModel counts, VarTensor factorMarginal, double multiplier) {
        // No op since this type of factor doesn't have any features.
    }

    public void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        // No op since this type of factor doesn't have any features.
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }
    
    protected abstract void createMessages(FgNode parent, Messages[] msgs, boolean logDomain);

}
