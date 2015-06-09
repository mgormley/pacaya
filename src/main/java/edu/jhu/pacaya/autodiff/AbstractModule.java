package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;

public abstract class AbstractModule<T extends MVec> implements Module<T> {

    protected T y;
    protected T yAdj;
    // The output adjoint will be represented in this abstract algebra.
    protected Algebra s;
    
    /** 
     * Constructor.
     * @param s The algebra of the output and output adjoint.
     */
    public AbstractModule(Algebra s) {
        this.s = s;
    }

    @Override
    public T getOutput() {
        return y;
    }

    @Override
    public T getOutputAdj() {
        if (yAdj == null) {
            yAdj = (T) y.copyAndFill(s.zero());
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

    public static <T extends MVec> void checkEqualAlgebras(Module<T> m1, Module<T> m2) {
        if (m1.getAlgebra().getClass() != m2.getAlgebra().getClass()) {
            throw new IllegalArgumentException("Algebras must be the same");
        }
    }
    
    public static <T extends MVec> void checkEqualAlgebras(Module<T> m1, Module<T> m2, Module<T> m3) {
        checkEqualAlgebras(m1, m2);
        checkEqualAlgebras(m2, m3);
    }
    
}