package edu.jhu.autodiff;

import java.util.List;

import edu.jhu.util.semiring.Algebra;


public interface Module<T extends MVec<? extends T>> {

    T forward();
    void backward();
    T getOutput();
    T getOutputAdj();
    void zeroOutputAdj();
    List<? extends Module<? extends MVec<?>>> getInputs();
    Algebra getAlgebra();
    
}
