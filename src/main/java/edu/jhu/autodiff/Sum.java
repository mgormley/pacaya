package edu.jhu.autodiff;

public class Sum extends AbstractModule {

    /** Foward pass: y = \sum_{i=1}^n x_i */
    @Override
    public Tensor forward(Tensor input) {
        Tensor y = new Tensor(1);        
        y.setValue(0, input.getSum());
        return y;
    }

    /** Backward pass: dG/dx_i = dG/dy dy/dx_i = dG/dy */
    @Override
    public Tensor backward(Tensor input, Tensor adjointIn) {
        Tensor adjointOut = new Tensor(input);
        adjointOut.add(adjointIn);
        return adjointOut;
    }

}
