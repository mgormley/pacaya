package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.Module;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * The module for creation of an explicit factor with no learnable parameters.
 * 
 * @author mgormley
 */
public class ParamFreeFactorModule extends AbstractModule<VarTensor> implements Module<VarTensor> {

    private Factor f;
    
    public ParamFreeFactorModule(Algebra s, Factor f) {
        super(s);
        this.f = f;
    }
    
    @Override
    public VarTensor forward() {
        y = BruteForceInferencer.safeNewVarTensor(s, f);
        return y;
    }

    @Override
    public void backward() {
        // No op.
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList();
    }
    
}