package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.collections.Lists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class TensorIdentity extends AbstractTensorModule implements Module<Tensor> {

    public TensorIdentity(Tensor y) {
        super(y.getAlgebra());
        this.y = y;
    }
    
    @Override
    public Tensor forward() {
        // No-op.
        return y;
    }

    @Override
    public void backward() {
        // No-op.
    }

    public List<Module<Tensor>> getInputs() {
        return Lists.getList();
    }

}
