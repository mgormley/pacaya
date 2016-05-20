package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;

public abstract class AbstractMutableModule<T extends MVec> extends AbstractModule<T> implements MutableModule<T> {

    public AbstractMutableModule(Algebra s) {
        super(s);
    }

    @Override
    public void setOutput(T y) {
        this.y = y;
    }

    @Override
    public void setOutputAdj(T yAdj) {
        this.yAdj = yAdj;
    }
    
}
