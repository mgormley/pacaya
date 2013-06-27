package edu.jhu.optimize;

import org.apache.log4j.Logger;

import edu.jhu.util.Utilities;

/**
 * Stochastic gradient descent.
 * 
 * We use the learning rate from "Introduction to Stochastic Search and
 * Optimization (James Spall). See eq. (4.14) pg. 113 and the constants
 * suggested on page pg. 164.
 * 
 * @author mgormley
 */
public class SGD implements Maximizer, Minimizer {

    /** Options for this optimizer. */
    public static class SGDPrm {
        /** The desired learning rate after (maxIterations / 2) iterations. */
        public double lrAtMidpoint = 1;
        /** The number of iterations to perform. */
        public int iterations = 10;
        /** The optimization will stop when this convergence tolerance is reached. */
        public double convergenceTolerance;
        public SGDPrm() { } 
        public SGDPrm(double lrAtMidpoint, int iterations) {
            this.lrAtMidpoint = lrAtMidpoint;
            this.iterations = iterations;
        }
    }
    
    private static final Logger log = Logger.getLogger(SGD.class);

    /** The number of iterations performed thus far. */
    private int iterCount;

    /** The parameters used to determine the next learning rate, given the current one. */
    private final double stepSize;
    private final double alpha;
    private final double A;

    private SGDPrm prm;
    
    /**
     * Constructs an SGD optimizer. 
     * 
     * @param lrAtMidpoint The desired learning rate after (maxIterations / 2) iterations.
     * @param iterations The number of iterations to perform.
     */
    public SGD(double lrAtMidpoint, int iterations) {
        this(new SGDPrm(lrAtMidpoint, iterations));
    }
    
    public SGD(SGDPrm prm) {
        this.prm = prm;
        iterCount = 0;

        // Constants
        this.A = 0.10 * prm.iterations;
        this.alpha = 0.602;
        this.stepSize = prm.lrAtMidpoint * Math.pow(prm.iterations / 2 + 1 + A, alpha);
        log.info("Initial step size parameter: " + this.stepSize);
    }

    /**
     * Updates the learning rate for the next iteration.
     */
    protected double getLearningRate(int iterCount) {
        //       >> a_k = a / (k + 1 + A)^\alpha
        //       >>
        //       >> where a is the step size, k is the iteration, A is a "stability
        //       >> constant", and \alpha is there so that in later iterations you get
        //       >> larger step sizes.  \alpha should be in (0.5,1].  \alpha=0.602 is
        //       >> suggested in the book, with some heuristic motivation. The step size
        //       >> is the most important parameter, which is tuned based on the "desired"
        //       >> change in magnitude in the early iterations.
        //       >>
        //       >> The only weird thing is the stability constant.  Sometimes you want
        //       >> larger step sizes "a" so that you have non-negligible step sizes after
        //       >> the algorithm has been running for a while.  But if they are too big
        //       >> you may get instability in the early iterations of the algorithm.  A
        //       >> is there to counter-balance that.   The heuristic guideline for
        //       >> setting A is 10% of the total number of allowed / expected iterations.
        return stepSize / Math.pow(iterCount + 1 + A, alpha);
    }

    /**
     * Maximize the function starting at the given initial point.
     */
    @Override
    public double[] maximize(Function function, double[] initial) {
        return optimize(function, initial, true);
    }

    /**
     * Minimize the function starting at the given initial point.
     */
    public double[] minimize(Function function, double[] initial) {
        return optimize(function, initial, false);
    }

    private double[] optimize(Function function, double[] initial, final boolean maximize) {
        double[] point = Utilities.copyOf(initial);
        for (iterCount=0; iterCount < prm.iterations; iterCount++) {
            // Get the current value of the function.
            double value = function.getValue(point);
            log.info(String.format("Function value = %g at iteration = %d", value, iterCount));
            
            // Get the gradient of the function.
            double[] gradient = function.getGradient(point);
            assert (gradient.length == point.length);
            
            // Take a step in the direction of the gradient.
            double lr = getLearningRate(iterCount);
            for (int i=0; i<point.length; i++) {
                if (maximize) {
                    point[i] += lr * gradient[i];
                } else {
                    point[i] -= lr * gradient[i];
                }
            }            
        }
        // Get the final value of the function.
        double value = function.getValue(point);
        log.info(String.format("Function value = %g at iteration = %d", value, iterCount));
        
        return point;
    }

    @Override
    public boolean wasMaxima() {
        // TODO: we don't test for convergence.
        return false;
    }

    @Override
    public boolean wasMinima() {
        // TODO: we don't test for convergence.
        return false;
    }
    

}
