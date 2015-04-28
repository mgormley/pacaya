package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * The module for creation of an explicit factor with no learnable parameters.
 * 
 * @author mgormley
 */
public class ParamFreeGlobalFactorModule extends AbstractModule<LazyVarTensor> implements Module<LazyVarTensor> {

    private GlobalFactor f;
    private List<? extends Module<? extends MVec>> inputs;
    
    public ParamFreeGlobalFactorModule(Algebra s, GlobalFactor f, List<? extends Module<? extends MVec>> inputs) {
        super(s);
        this.f = f;
        this.inputs = inputs;
    }
    
    @Override
    public LazyVarTensor forward() {
        y = new LazyVarTensor(f, s);
        return y;
    }

    @Override
    public void backward() {
        // No op.
        //
        // The output (i.e. return value of forward()) should never be accessed directly. The entire
        // point of the GlobalFactor interface is to avoid instantiating the massive tensor that 
        // the factor represents.
    }
    
    @Override
    public LazyVarTensor getOutputAdj() {
        throw new RuntimeException("This method fails when called since any modifications to this adjoint would require us to implement backward().");
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return inputs;
    }
    
}