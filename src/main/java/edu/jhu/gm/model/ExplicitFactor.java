package edu.jhu.gm.model;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.util.semiring.Algebras;


/**
 * Factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExplicitFactor extends VarTensor implements Factor {

    private static final long serialVersionUID = 1L;
    
    private int id = -1;
    
    public ExplicitFactor(VarSet vars) {
        super(Algebras.REAL_ALGEBRA, vars);
    }
    
    public ExplicitFactor(ExplicitFactor other) {
        super(other);
    }
    
    public ExplicitFactor(VarTensor other) {
        super(other);
    }

    public ExplicitFactor getClamped(VarConfig clmpVarConfig) {
        VarTensor df = super.getClamped(clmpVarConfig);
        return new ExplicitFactor(df);
    }
    
    public void updateFromModel(FgModel model) {
        // No op since this type of factor doesn't depend on the model.
    }
    
    public double getLogUnormalizedScore(int configId) {
        return this.getValue(configId);
    }

    public double getLogUnormalizedScore(VarConfig vc) {
        return getLogUnormalizedScore(vc.getConfigIndex());
    }

    public void addExpectedFeatureCounts(IFgModel counts, VarTensor factorMarginal, double multiplier) {
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

}
