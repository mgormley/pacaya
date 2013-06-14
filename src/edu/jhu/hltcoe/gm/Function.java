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
     * Gets the derivative at the given point.
     * @param point The point.
     * @return The derivative.
     */
    double[] getDerivative(double[] point);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
}
