package edu.jhu.gm;

/**
 * Factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExplicitFactor extends DenseFactor implements Factor {

    protected Object templateKey;

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
    

}
