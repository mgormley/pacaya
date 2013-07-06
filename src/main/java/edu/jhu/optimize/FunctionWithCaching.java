package edu.jhu.optimize;

/**
 * A differentiable function.
 * 
 * @author mgormley
 *
 */
public interface FunctionWithCaching {

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
     * @return The gradient, a vector of partial derivatives.
     */
    double[] getGradient();
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
}
