package edu.jhu.pacaya.autodiff;

import java.util.List;

import edu.jhu.pacaya.util.collections.QLists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class Identity<T extends MVec> extends AbstractMutableModule<T> implements Module<T>, MutableModule<T> {

    public Identity(T y) {
        super(y.getAlgebra());
        this.y = y;
    }
    
    @Override
    public T forward() {
        // No-op.
        return y;
    }

    @Override
    public void backward() {
        // No-op.
    }

    public List<Module<?>> getInputs() {
        return QLists.getList();
    }

}
