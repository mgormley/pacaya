package edu.jhu.autodiff;


public interface Module<T> {

    T forward();
    void backward();    
    T getOutput();
    T getOutputAdj();
    
}
