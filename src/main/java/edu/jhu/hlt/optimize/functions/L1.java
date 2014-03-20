package edu.jhu.hlt.optimize.functions;

import edu.jhu.hlt.optimize.function.Regularizer;
import edu.jhu.prim.arrays.DoubleArrays;

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
    private double[] params;
    
    public L1(double lambda) {
        this.lambda = lambda;
    }
    
    public void setPoint(double[] params) {
        this.params = params;
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
     * Gets - \lambda * |\theta|_1.
     */
    @Override
    public double getValue() {
        double sum = 0.0;
        for (int i=0; i<params.length; i++) {
            sum += Math.abs(params[i]);
        }
        return - lambda * sum;
    }

    @Override
    public void getGradient(double[] gradient) {
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
        // Since we're subtracting this norm.
        DoubleArrays.scale(gradient, -1);
    }

    @Override
    public int getNumDimensions() {
        return numParams;
    }
    
    public void setNumDimensions(int numParams) {
        this.numParams = numParams ;
    }

}
