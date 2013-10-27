package edu.jhu.gm;

/**
 * Exponential family factor represented as a dense conditional probability
 * table.
 * 
 * @author mgormley
 */
// TODO: At the moment, this is just an IDENTIFIER for exponential family
// factors...the actual behavior of this class is identical to ExplicitFactor.
public class ExpFamFactor extends ExplicitFactor implements Factor {
    
    private static final long serialVersionUID = 1L;

    public ExpFamFactor(VarSet vars, Object templateKey) {
        super(vars, templateKey);
    }
    
    public ExpFamFactor(ExpFamFactor other) {
        super(other);
    }
    
    public ExpFamFactor(DenseFactor other, Object templateId) {
        super(other, templateId);
    }

    public ExpFamFactor getClamped(VarConfig clmpVarConfig) {
        DenseFactor df = super.getClamped(clmpVarConfig);
        return new ExpFamFactor(df, templateKey);
    }
    
}
