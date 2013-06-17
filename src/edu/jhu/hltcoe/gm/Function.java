package edu.jhu.hltcoe.gm;

/**
 * A differentiable function.
 * 
 * @author mgormley
 *
 */
public interface Function {

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
    double[] getGradient(double[] point);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
}
