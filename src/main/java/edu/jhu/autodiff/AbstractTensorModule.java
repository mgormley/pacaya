package edu.jhu.autodiff;

import edu.jhu.util.semiring.Algebra;

public abstract class AbstractTensorModule implements Module<Tensor> {

    protected Tensor y;
    protected Tensor yAdj;
    // The output and output adjoint will be represented in this abstract algebra.
    protected Algebra s;
    
    public AbstractTensorModule(Algebra s) {
        this.s = s;
    }

    @Override
    public Tensor getOutput() {
        return y;
    }

    @Override
    public Tensor getOutputAdj() {
        if (yAdj == null) {
            yAdj = y.copyAndFill(s.zero());
        }
        return yAdj;
    }
    
    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) { yAdj.fill(s.zero()); }
    }

    @Override
    public Algebra getAlgebra() {
        return s;
    }

    @Override
    public String toString() {
        return this.getClass() + " [y=" + y + ", yAdj=" + yAdj + "]";
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