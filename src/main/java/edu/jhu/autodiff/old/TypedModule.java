package edu.jhu.autodiff.old;

public interface TypedModule<X,Y> {

    /** 
     * Computes the forward pass.
     * 
     * @param input The input variable values.
     * @return The output variable values. 
     */    
    Y forward(X input);
    
    /** 
     * Computes the backward pass.
     * 
     * @param input The input variable values (expects the same input as forward call).
     * @param outputAdj The adjoints with respect to the output variables of this module.
     * @return The adjoints with respect to the input variables of this module.
     */
    X backward(X input, Y outputAdj);
    
}
