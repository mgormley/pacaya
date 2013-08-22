package edu.jhu.optimize;

/**
 * A first order optimization technique for maximization of a function which can
 * be computed over batches of examples.
 * 
 * @author mgormley
 * 
 */
public interface BatchMaximizer {

    /**
     * Maximizes a "batchable" function starting from some initial point.
     * 
     * @param function The function to optimize.
     * @param point The input/output point. The initial point for maximization
     *            should be passed in. When this method returns this parameter
     *            will contain the point at which the maximizer terminated,
     *            possibly the maximum.
     * @return True if the optimizer terminated at a local or global optima. False otherwise.
     */
    boolean maximize(BatchFunction function, double[] point);
    
}
