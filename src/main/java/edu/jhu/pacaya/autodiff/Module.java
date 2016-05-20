package edu.jhu.pacaya.autodiff;

import java.util.List;

import edu.jhu.pacaya.util.semiring.Algebra;


public interface Module<T extends MVec> {

    T forward();
    void backward();
    T getOutput();
    T getOutputAdj();
    void zeroOutputAdj();
    List<? extends Module<? extends MVec>> getInputs();
    Algebra getAlgebra();
    
}
