package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractMutableModule;
import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.MVecArray;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.MutableModule;
import edu.jhu.util.collections.Lists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class MVecArrayIdentity<T extends MVec> extends AbstractMutableModule<MVecArray<T>> implements MutableModule<MVecArray<T>> {
    
    public MVecArrayIdentity(MVecArray<T> y) {
        super(y.s);
        this.y = y;
    }
    
    @Override
    public MVecArray<T> forward() {
        // No-op.
        return y;
    }

    @Override
    public void backward() {
        // No-op.
    }

    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList();
    }

}
