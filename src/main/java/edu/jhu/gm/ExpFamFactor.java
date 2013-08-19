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

    public ExpFamFactor(VarSet vars, Object templateKey) {
        super(vars, templateKey);
    }
    
    public ExpFamFactor(ExpFamFactor other) {
        super(other);
    }
    
    public ExpFamFactor(DenseFactor other, Object templateId) {
        super(other, templateId);
    }

}
