package edu.jhu.gm.model;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.FgInferencer;


/**
 * Factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExplicitFactor extends DenseFactor implements Factor {

    private static final long serialVersionUID = 1L;

    // The unique key identifying the template for this factor.
    protected Object templateKey;
    // The ID of the template for this factor -- which is only ever set by the
    // FeatureTemplateList.
    private int templateId = -1;
        
    public ExplicitFactor(VarSet vars, Object templateKey) {
        super(vars);
        this.templateKey = templateKey;
    }
    
    public ExplicitFactor(ExplicitFactor other) {
        super(other);
        this.templateKey = other.templateKey;               
    }
    
    public ExplicitFactor(DenseFactor other, Object templateId) {
        super(other);
        this.templateKey = templateId;               
    }

    public ExplicitFactor getClamped(VarConfig clmpVarConfig) {
        DenseFactor df = super.getClamped(clmpVarConfig);
        return new ExplicitFactor(df, templateKey);
    }

    @Override
    public Object getTemplateKey() {
        return templateKey;
    }
    
    public int getTemplateId() {
        return templateId;
    }
    
    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }
    
    public void updateFromModel(FgModel model, boolean logDomain) {
        // No op since this type of factor doesn't depend on the model.
    }
    
    public double getUnormalizedScore(int configId) {
        return this.getValue(configId);
    }

    public void addExpectedFeatureCounts(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        // No op since this type of factor doesn't have any features.
    }

}
