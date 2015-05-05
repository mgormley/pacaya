package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class FgModelIdentity extends AbstractModule<MVecFgModel> implements Module<MVecFgModel> {

    public FgModelIdentity(FgModel model) {
        super(RealAlgebra.REAL_ALGEBRA);
        this.y = new MVecFgModel(model);
        assert this.y.getAlgebra() == RealAlgebra.REAL_ALGEBRA;
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
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList();
    }


}
