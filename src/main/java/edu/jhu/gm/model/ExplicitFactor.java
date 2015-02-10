package edu.jhu.gm.model;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.erma.AutodiffFactor;
import edu.jhu.autodiff.erma.MVecFgModel;
import edu.jhu.autodiff.erma.ParamFreeFactorModule;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;


/**
 * Factor represented as a dense conditional probability table.
 * 
 * @author mgormley
 */
public class ExplicitFactor extends VarTensor implements Factor, AutodiffFactor {

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

    @Override
    public Module<VarTensor> getFactorModule(Module<MVecFgModel> modIn, Algebra s) {
        return new ParamFreeFactorModule(s, this);
    }

}
