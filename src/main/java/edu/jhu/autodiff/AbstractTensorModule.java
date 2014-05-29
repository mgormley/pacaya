package edu.jhu.autodiff;

public abstract class AbstractTensorModule implements Module<Tensor> {

    protected Tensor y;
    protected Tensor yAdj;

    public AbstractTensorModule() {
        super();
    }

    @Override
    public Tensor getOutput() {
        return y;
    }

    @Override
    public Tensor getOutputAdj() {
        if (yAdj == null) {
            yAdj = y.copyAndFill(0);
        }
        return yAdj;
    }

    @Override
    public String toString() {
        return this.getClass() + " [y=" + y + ", yAdj=" + yAdj + "]";
    }

    
    
}