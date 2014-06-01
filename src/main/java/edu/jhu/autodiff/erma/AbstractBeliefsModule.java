package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.util.semiring.Algebra;

public abstract class AbstractBeliefsModule implements Module<Beliefs> {

    protected Beliefs b;
    protected Beliefs bAdj;
    protected Algebra s;
    
    public AbstractBeliefsModule(Algebra s) {
        this.s = s;
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
    public Algebra getAlgebra() {
        return s;
    }

    @Override
    public String toString() {
        return this.getClass() + " [b=" + b + ", bAdj=" + bAdj + "]";
    } 
    
    public static void checkEqualAlgebras(Module<Tensor> m1, Module<Tensor> m2) {
        if (m1.getAlgebra().getClass() != m2.getAlgebra().getClass()) {
            throw new IllegalArgumentException("Algebras must be the same");
        }
    }
    
    public static void checkEqualAlgebras(Module<Tensor> m1, Module<Tensor> m2, Module<Tensor> m3) {
        checkEqualAlgebras(m1, m2);
        checkEqualAlgebras(m2, m3);
    }
}