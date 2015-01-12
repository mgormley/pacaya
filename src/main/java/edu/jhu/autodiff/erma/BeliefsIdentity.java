package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractModule;
import edu.jhu.autodiff.Module;
import edu.jhu.util.collections.Lists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class BeliefsIdentity extends AbstractModule<Beliefs> implements Module<Beliefs> {
    
    public BeliefsIdentity(Beliefs b) {
        super(b.s);
        this.y = b;
    }
    
    @Override
    public Beliefs forward() {
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
