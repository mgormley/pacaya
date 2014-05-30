package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;

public abstract class AbstractBeliefsModule implements Module<Beliefs> {

    protected Beliefs b;
    protected Beliefs bAdj;

    public AbstractBeliefsModule() {
        super();
    }

    @Override
    public Beliefs getOutput() {
        return b;
    }

    @Override
    public Beliefs getOutputAdj() {
        if (bAdj == null) {
            bAdj = b.copyAndFill(0.0);
        }
        return bAdj;
    }
    
    @Override
    public void zeroOutputAdj() {
        if (bAdj != null) { bAdj.fill(0.0); }
    }

    @Override
    public String toString() {
        return this.getClass() + " [b=" + b + ", bAdj=" + bAdj + "]";
    }    
    
}