package edu.jhu.autodiff;

public abstract class AbstractModule implements Module {

    @Override
    public abstract Tensor forward(Tensor input);

    @Override
    public abstract Tensor backward(Tensor input, Tensor adjointIn);

    @Override
    public void accumGradient(Tensor gradient) {
        // No-op.
    }

    @Override
    public void zeroGradient() {
        // No-op.
    }

}
