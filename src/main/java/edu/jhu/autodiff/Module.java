package edu.jhu.autodiff;

import java.util.List;


public interface Module<T> {

    T forward();
    void backward();
    T getOutput();
    T getOutputAdj();
    void zeroOutputAdj();
    List<? extends Object> getInputs();
    
}
