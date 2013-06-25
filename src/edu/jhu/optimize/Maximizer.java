package edu.jhu.optimize;

/**
 * A first order optimization technique for maximization.
 * 
 * @author mgormley
 *
 */
public interface Maximizer {

    /**
     * Maximizes a function starting from some initial point.
     * 
     * @param function
     * @param initial
     * @return The point at which the maximizer terminated, possibly the maximum.
     */
    double[] maximize(Function function, double[] initial);
    
    /**
     * @return True if the optimizer terminated at a local or global optima. False otherwise.
     */
    boolean wasMaxima();
    
}
