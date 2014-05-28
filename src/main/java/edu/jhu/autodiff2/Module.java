package edu.jhu.autodiff2;


public interface Module<T> {

    T forward();
    void backward();    
    T getOutput();
    T getOutputAdj();
    
}
