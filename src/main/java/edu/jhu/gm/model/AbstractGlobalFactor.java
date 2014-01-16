package edu.jhu.gm.model;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.FactorGraph.FgNode;

public abstract class AbstractGlobalFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
    // The ID of the template for this factor -- which is only ever set by the
    // FeatureTemplateList.
    private int templateId = -1;
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

    public int getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public void updateFromModel(FgModel model, boolean logDomain) {
        // Currently, global factors do not support features, and
        // therefore have no model parameters.
    }   

    public void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        // No op since this type of factor doesn't have any features.
    }
    
    protected abstract void createMessages(FgNode parent, Messages[] msgs, boolean logDomain);

}
