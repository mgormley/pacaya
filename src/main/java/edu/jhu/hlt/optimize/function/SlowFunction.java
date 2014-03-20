package edu.jhu.hlt.optimize.function;

/**
 * A differentiable function.
 * 
 * @author mgormley
 *
 */
public interface SlowFunction {

    /**
     * The value of this function at the given point.
     * @param point The point.
     * @return The value of the function at the point.
     */
    double getValue(double[] point);
    
    /**
     * Gets the gradient at the given point.
     * @param point The point.
     * @return The gradient, a vector of partial derivatives.
     */
    double[] getGradientAtPoint(double[] point);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
}
