package edu.jhu.optimize;


/**
 * A differentiable function, the gradient of which can be computed on a subset
 * of the examples, but summing over the individual examples.
 * 
 * @author mgormley
 * 
 */
public abstract class AbstractSumBatchFunction implements BatchFunction {

    /* --------------- Implemented Methods from BatchFunction --------------- */

    /**
     * Gets value of this function at the current point, computed on the given batch of examples.
     * @param batch A set of indices indicating the examples over which the gradient should be computed.
     * @return The value of the function at the point.
     */
    public double getValue(int[] batch) {
        double value = 0.0;
        for (int i=0; i<batch.length; i++) {
            value += getValue(i);
        }
        return value;
    }
    
    /**
     * Gets the gradient at the current point, computed on the given batch of examples.
     * @param batch A set of indices indicating the examples over which the gradient should be computed.
     * @param gradient The output gradient, a vector of partial derivatives.
     */
    public void getGradient(int[] batch, double[] gradient) {
        for (int i=0; i<batch.length; i++) {
            addGradient(i, gradient);
        }
    }
    
    /* --------------- New Abstract Methods for this class --------------- */
    
    /**
     * Adds the gradient at the current point, computed on the given example.
     * @param i The index of the example.
     * @param gradient The output gradient, a vector of partial derivatives.
     */
    public abstract void addGradient(int i, double[] gradient);
    
    /**
     * Sets the current point for this function.
     * @param point The point.
     */
    public abstract void setPoint(double[] point);

    /* --------------- Unimplemented Methods from BatchFunction --------------- */
    
    /**
     * Gets value of this function at the current point, computed on the given example.
     * @param i The index of the example.
     * @return The value of the function at the point.
     */
    public abstract double getValue(int i);
    
    /**
     * Gets the number of dimensions of the domain of this function.
     * 
     * @return The domain's dimensionality.
     */
    public abstract int getNumDimensions();
    
    /**
     * Gets the number of examples.
     */
    public abstract int getNumExamples();
}

