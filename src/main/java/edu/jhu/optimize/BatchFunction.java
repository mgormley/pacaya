package edu.jhu.optimize;


/**
 * A differentiable function, the gradient of which can be computed on a subset of the examples.
 * 
 * @author mgormley
 *
 */
public interface BatchFunction {

    /**
     * Sets the current point for this function.
     * @param point The point.
     */
    void setPoint(double[] point);
    
    /**
     * Gets value of this function at the current point, computed on the given batch of examples.
     * @param batch A set of indices indicating the examples over which the gradient should be computed.
     * @return The value of the function at the point.
     */
    double getValue(int[] batch);
    
    /**
     * Adds the gradient at the current point, computed on the given batch of examples.
     * @param batch A set of indices indicating the examples over which the gradient should be computed.
     * @param gradient The output gradient, a vector of partial derivatives.
     */
    void getGradient(int[] batch, double[] gradient);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    int getNumDimensions();
    
    /**
     * Gets the number of examples.
     */
    int getNumExamples();
}

