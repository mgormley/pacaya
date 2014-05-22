package edu.jhu.gm.model;

import edu.jhu.gm.inf.FgInferencer;


/**
 * Factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExplicitFactor extends VarTensor implements Factor {

    private static final long serialVersionUID = 1L;
    
    private int id = -1;
    
    public ExplicitFactor(VarSet vars) {
        super(vars);
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
    
    public void updateFromModel(FgModel model, boolean logDomain) {
        // No op since this type of factor doesn't depend on the model.
    }
    
    public double getUnormalizedScore(int configId) {
        return this.getValue(configId);
    }

    public double getUnormalizedScore(VarConfig vc) {
        return getUnormalizedScore(vc.getConfigIndex());
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

}
