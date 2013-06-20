package edu.jhu.hltcoe.gm;

import edu.jhu.hltcoe.util.math.Vectors;

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
     * Gets the sum of squares times 1/(2\sigma^2).
     */
    @Override
    public double getValue(double[] params) {
        double sum = Vectors.dotProduct(params, params);
        sum /= (2 * variance);
        return sum;
    }

    /**
     * Gets the parameter value times 1/(\sigma^2).
     */
    // TODO: Why do Sutton & McCallum include the sum of the parameters here and not just the value for each term of the gradient.
    @Override
    public double[] getGradient(double[] params) {
        double[] gradient = new double[params.length];
        for (int j=0; j<gradient.length; j++) {
            gradient[j] = params[j] / (variance);
        }
        return gradient;
    }

    @Override
    public int getNumDimensions() {
        return numParams;
    }

}
