package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;

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