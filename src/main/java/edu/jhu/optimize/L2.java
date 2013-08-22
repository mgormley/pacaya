package edu.jhu.optimize;

import edu.jhu.util.math.Vectors;

/**
 * Gaussian prior (L2 regularizer) on the parameters with mean zero and a
 * specified variance.
 * 
 * @author mgormley
 * 
 */
public class L2 implements Regularizer {

    private double variance;
    private int numParams;
    private double[] params;
    
    public L2(double variance) {
        this.variance = variance;
    }
    
    public void setPoint(double[] params) {
        this.params = params;
    }
    
    /**
     * Builds a Gaussian prior (L2 regularizer) on the parameters.
     * 
     * @param variance The covariance matrix of the Gaussian will be variance*I.
     * @param numParams The number of parameters.
     */
    public L2(double variance, int numParams) {
        this.variance = variance;
        this.numParams = numParams;
    }
    
    /**
     * Gets the negated sum of squares times 1/(2\sigma^2).
     */
    @Override
    public double getValue() {
        double sum = Vectors.dotProduct(params, params);
        sum /= (2 * variance);
        return - sum;
    }

    /**
     * Gets the negative parameter value times 1/(\sigma^2).
     */
    // TODO: Why do Sutton & McCallum include the sum of the parameters here and not just the value for each term of the gradient.
    @Override
    public void getGradient(double[] gradient) {
        for (int j=0; j<gradient.length; j++) {
            gradient[j] = - params[j] / variance;
        }
    }

    @Override
    public int getNumDimensions() {
        return numParams;
    }

    public void setNumDimensions(int numParams) {
        this.numParams = numParams ;
    }

}
