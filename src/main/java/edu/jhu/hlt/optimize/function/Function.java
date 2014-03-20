package edu.jhu.hlt.optimize.function;

/**
 * A differentiable function.
 * 
 * @author mgormley
 *
 */
public interface Function {

    /**
     * Sets the current point for this function.
     * @param point The point.
     */
    void setPoint(double[] point);
    
    /**
     * The value of this function at the current point.
     * @return The value of the function.
     */
    double getValue();
    
    /**
     * Gets the gradient at the current point.
     * @param gradient The output gradient, a vector of partial derivatives.
     */
    void getGradient(double[] gradient);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
    
}
