package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class BeliefsIdentity extends AbstractBeliefsModule implements Module<Beliefs> {
    
    public BeliefsIdentity(Beliefs b) {
        super(b.s);
        this.b = b;
    }
    
    @Override
    public Beliefs forward() {
        // No-op.
        return b;
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
