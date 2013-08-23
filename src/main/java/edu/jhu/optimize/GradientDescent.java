package edu.jhu.optimize;

import java.util.Arrays;

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
public class GradientDescent implements Maximizer, Minimizer {

    /** Options for this optimizer. */
    public static class GradientDescentPrm {
        /** The desired learning rate after (maxIterations / 2) iterations. */
        public double lrAtMidpoint = 0.1;
        /** The number of iterations to perform. */
        public int iterations = 10;
        public GradientDescentPrm() { } 
        public GradientDescentPrm(double lrAtMidpoint, int iterations) {
            this.lrAtMidpoint = lrAtMidpoint;
            this.iterations = iterations;
        }
    }
    
    private static final Logger log = Logger.getLogger(GradientDescent.class);

    /** The number of iterations performed thus far. */
    private int iterCount;

    /** The parameters used to determine the next learning rate, given the current one. */
    private final double stepSize;
    private final double alpha;
    private final double A;

    private GradientDescentPrm prm;
    
    /**
     * Constructs an SGD optimizer. 
     * 
     * @param lrAtMidpoint The desired learning rate after (maxIterations / 2) iterations.
     * @param iterations The number of iterations to perform.
     */
    public GradientDescent(double lrAtMidpoint, int iterations) {
        this(new GradientDescentPrm(lrAtMidpoint, iterations));
    }
    
    public GradientDescent(GradientDescentPrm prm) {
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
    public boolean maximize(Function function, double[] point) {
        return optimize(function, point, true);
    }

    /**
     * Minimize the function starting at the given initial point.
     */
    public boolean minimize(Function function, double[] point) {
        return optimize(function, point, false);
    }

    private boolean optimize(Function function, double[] point, final boolean maximize) {        
        assert (function.getNumDimensions() == point.length);
        double[] gradient = new double[point.length];
        
        int passCount = 0;
        double passCountFrac = 0;
        for (iterCount=0; iterCount < prm.iterations; iterCount++) {
            function.setPoint(point);
            
            // Get the current value of the function.
            double value = function.getValue();
            log.info(String.format("Function value on batch = %g at iteration = %d", value, iterCount));
            
            // Get the gradient of the function.
            Arrays.fill(gradient, 0.0);
            function.getGradient(gradient);
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
        
        // Get the final value of the function on all the examples.
        double value = function.getValue();
        log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
        
        // We don't test for convergence.
        return false;
    }
    
}
