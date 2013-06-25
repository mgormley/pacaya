package edu.jhu.optimize;

/**
 * A first order optimization technique for minimization.
 * 
 * @author mgormley
 *
 */
public interface Minimizer {

    /**
     * Minimizes a function starting from some initial point.
     * 
     * @param function
     * @param initial
     * @return The point at which the minizer terminated, possibly the minimum.
     */
    double[] minimize(Function function, double[] initial);
    
    /**
     * @return True if the optimizer terminated at a local or global optima. False otherwise.
     */
    boolean wasMinima();
    
}
