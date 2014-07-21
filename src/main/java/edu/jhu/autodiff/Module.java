package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.semiring.Algebra;


public interface Module<T extends ModuleTensor> {

    T forward();
    void backward();
    T getOutput();
    T getOutputAdj();
    void zeroOutputAdj();
    List<? extends Module<? extends ModuleTensor>> getInputs();
    Algebra getAlgebra();
    
}
