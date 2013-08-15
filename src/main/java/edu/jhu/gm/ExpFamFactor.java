package edu.jhu.gm;

/**
 * Exponential family factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExpFamFactor extends DenseFactor implements Factor {

    private Object templateKey;

    public ExpFamFactor(VarSet vars, Object templateKey) {
        super(vars);
        this.templateKey = templateKey;
    }
    
    public ExpFamFactor(ExpFamFactor other) {
        super(other);
        this.templateKey = other.templateKey;               
    }
    
    public ExpFamFactor(DenseFactor other, Object templateId) {
        super(other);
        this.templateKey = templateId;               
    }

    public ExpFamFactor getClamped(VarConfig clmpVarConfig) {
        DenseFactor df = super.getClamped(clmpVarConfig);
        return new ExpFamFactor(df, templateKey);
    }

    @Override
    public Object getTemplateKey() {
        return templateKey;
    }
    

}
