package edu.jhu.optimize;

/**
 * A first order optimization technique for minimization of a function which can
 * be computed over batches of examples.
 * 
 * @author mgormley
 * 
 */
public interface BatchMinimizer {

    /**
     * Minimizes a "batchable" function starting from some initial point.
     * 
     * @param function The function to optimize.
     * @param point The input/output point. The initial point for minimization
     *            should be passed in. When this method returns this parameter
     *            will contain the point at which the minimizer terminated,
     *            possibly the minimum.
     * @return True if the optimizer terminated at a local or global optima. False otherwise.
     */
    boolean minimize(BatchFunction function, double[] point);
    
    /** Gets the number of examples that will be included in each batch. */
    int getBatchSize();

}
