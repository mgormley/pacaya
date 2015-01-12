package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.Module;
import edu.jhu.gm.model.FgModel;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebras;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class FgModelIdentity extends AbstractModule<MVecFgModel> implements Module<MVecFgModel> {

    public FgModelIdentity(FgModel model) {
        super(Algebras.REAL_ALGEBRA);
        this.y = new MVecFgModel(model);
        assert this.y.getAlgebra() == Algebras.REAL_ALGEBRA;
    }
    
    @Override
    public MVecFgModel forward() {
        // No-op.
        return y;
    }

    @Override
    public void backward() {
        // No-op.
    }
    
    @Override
    public List<? extends Module<? extends MVec<?>>> getInputs() {
        return Lists.getList();
    }


}
