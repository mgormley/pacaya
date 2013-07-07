package edu.jhu.optimize;

/**
 * L1 regularizer on the parameters.
 * 
 * <p>
 * Of course, the L1 regularizer isn't actually differentiable at 0. While L1
 * regularization can be written out as a convex quadratic program and solved
 * with a generic solver, we instead simply return 0 for the case of the
 * gradient at 0 as a simple hack.
 * </p>
 * 
 * @author mgormley
 */
public class L1 implements Regularizer {

    private double lambda;
    private int numParams;

    public L1(double lambda) {
        this.lambda = lambda;
    }
    
    /**
     * Builds an L1 regularizer on the parameters.
     * 
     * @param lambda The multiplier on the L1 regularization term.
     * @param numParams The number of parameters.
     */
    public L1(double lambda, int numParams) {
        this.lambda = lambda;
        this.numParams = numParams;
    }
    
    /**
     * Gets \lambda * |\theta|_1.
     */
    @Override
    public double getValue(double[] params) {
        double sum = 0.0;
        for (int i=0; i<params.length; i++) {
            sum += Math.abs(params[i]);
        }
        return lambda * sum;
    }

    @Override
    public double[] getGradient(double[] params) {
        double[] gradient = new double[params.length];
        for (int j=0; j<gradient.length; j++) {
            if (params[j] < 0) {
                gradient[j] = - lambda;
            } else if (params[j] > 0) {
                gradient[j] = lambda;
            } else {
                // This is just a hack to work around the fact that L1 is not
                // differentiable at zero.
                gradient[j] = 0;
                //throw new RuntimeException("The derivative is undefined at zero.");
            }
        }
        return gradient;
    }

    @Override
    public int getNumDimensions() {
        return numParams;
    }
    
    public void setNumDimensions(int numParams) {
        this.numParams = numParams ;
    }

}