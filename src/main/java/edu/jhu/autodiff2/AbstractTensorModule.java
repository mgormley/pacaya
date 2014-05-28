package edu.jhu.autodiff2;

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

}