package edu.jhu.autodiff.erma;

import edu.jhu.autodiff2.Module;
import edu.jhu.autodiff2.Tensor;

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
            yAdj = y.copyAndFill(0);
        }
        return yAdj;
    }


}
