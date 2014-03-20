package edu.jhu.hlt.optimize.function;



public interface Regularizer extends Function {
    
    void setNumDimensions(int numParams);
    
}
