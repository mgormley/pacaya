package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.Module;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;

/**
 * The module for creation of an explicit factor with no learnable parameters.
 * 
 * @author mgormley
 */
public class ParamFreeGlobalFactorModule extends AbstractModule<LazyVarTensor> implements Module<LazyVarTensor> {

    private GlobalFactor f;
    
    public ParamFreeGlobalFactorModule(Algebra s, GlobalFactor f) {
        super(s);
        this.f = f;
    }
    
    @Override
    public LazyVarTensor forward() {
        y = new LazyVarTensor(f, s);
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