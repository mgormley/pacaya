package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.collections.Lists;

/**
 * This module is simply the identity function. 
 * @author mgormley
 */
public class BeliefsIdentity implements Module<Beliefs> {

    private Beliefs y;
    private Beliefs yAdj;
    
    public BeliefsIdentity(Beliefs y) {
        this.y = y;
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
    public Beliefs getOutput() {
        return y;
    }

    @Override
    public Beliefs getOutputAdj() {
        if (yAdj == null) {
            yAdj = y.copyAndFill(0.0);
        }
        return yAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) { yAdj.fill(0.0); }
    }

    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList();
    }

}
