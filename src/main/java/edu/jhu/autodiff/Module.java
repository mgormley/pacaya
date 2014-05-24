package edu.jhu.autodiff;

public interface Module {

    /** 
     * Computes the forward pass.
     * 
     * @param input The input variable values.
     * @return The output variable values. 
     */    
    Tensor forward(Tensor input);
    
    /** 
     * Computes the backward pass.
     * 
     * @param input The input variable values (expects the same input as forward call).
     * @param outputAdj The adjoints with respect to the output variables of this module.
     * @return The adjoints with respect to the input variables of this module.
     */
    Tensor backward(Tensor input, Tensor outputAdj);
    
    /**
     * Adds the given gradien to the internal accumulator.
     * 
     * @param gradient The gradient to accumulate.
     */
    void accumGradient(Tensor gradient);
    
    /**
     * Zeroes the internal gradient accumulator.
     */
    void zeroGradient();
}
