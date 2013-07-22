package edu.jhu.optimize;


public interface Regularizer extends Function {
    
    void setNumDimensions(int numParams);
    
}
