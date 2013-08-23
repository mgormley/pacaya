package edu.jhu.optimize;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;

/**
 * Stochastic gradient descent with minibatches.
 * 
 * We use the learning rate from "Introduction to Stochastic Search and
 * Optimization (James Spall). See eq. (4.14) pg. 113 and the constants
 * suggested on page pg. 164.
 * 
 * @author mgormley
 */
public class SGD implements BatchMaximizer, BatchMinimizer {

    /** Options for this optimizer. */
    public static class SGDPrm {
        /** The desired learning rate after (maxIterations / 2) iterations. */
        public double lrAtMidpoint = 0.1;
        /** The number of passes over the dataset to perform. */
        public int numPasses = 10;
        /** The batch size to use at each step. */
        public int batchSize = 15;
        public SGDPrm() { } 
        public SGDPrm(double lrAtMidpoint, int numPasses, int batchSize) {
            this.lrAtMidpoint = lrAtMidpoint;
            this.numPasses = numPasses;
            this.batchSize = batchSize;
        }
    }
    
    private static final Logger log = Logger.getLogger(SGD.class);

    /** The number of gradient steps to run. */   
    private int iterations;
    /** The number of iterations performed thus far. */
    private int iterCount;

    /** The parameters used to determine the next learning rate, given the current one. */
    private double stepSize;
    private double alpha;
    private double A;

    private SGDPrm prm;
    
    /**
     * Constructs an SGD optimizer.
     */
    public SGD(SGDPrm prm) {
        this.prm = prm;
    }
    
    /**
     * Initializes all the parameters for optimization.
     */
    private void init(BatchFunction function) {
        // Counters
        iterCount = 0;
                        
        // Constants
        int numExamples = function.getNumExamples();
        iterations = (int) Math.ceil((double) prm.numPasses * numExamples / prm.batchSize);
        log.info("Setting number of batch gradient steps: " + iterations);
        this.A = 0.10 * iterations;
        this.alpha = 0.602;
        this.stepSize = prm.lrAtMidpoint * Math.pow(iterations / 2 + 1 + A, alpha);
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
    public boolean maximize(BatchFunction function, double[] point) {
        return optimize(function, point, true);
    }

    /**
     * Minimize the function starting at the given initial point.
     */
    public boolean minimize(BatchFunction function, double[] point) {
        return optimize(function, point, false);
    }

    private boolean optimize(BatchFunction function, double[] point, final boolean maximize) {
        init(function);
        
        assert (function.getNumDimensions() == point.length);
        double[] gradient = new double[point.length];
        
        int passCount = 0;
        double passCountFrac = 0;
        for (iterCount=0; iterCount < iterations; iterCount++) {
            function.setPoint(point);
            
            int[] batch = getBatch(function.getNumExamples());
            
            // Get the current value of the function.
            double value = function.getValue(batch);
            log.trace(String.format("Function value on batch = %g at iteration = %d", value, iterCount));
            
            // Get the gradient of the function.
            Arrays.fill(gradient, 0.0);
            function.getGradient(batch, gradient);
            assert (gradient.length == point.length);
            
            // Take a step in the direction of the gradient.
            double lr = getLearningRate(iterCount);
            log.trace("Learning rate: " + lr);
            for (int i=0; i<point.length; i++) {
                if (maximize) {
                    point[i] += lr * gradient[i];
                } else {
                    point[i] -= lr * gradient[i];
                }
            }
            
            // If a full pass through the data has been completed...
            passCountFrac = (double) iterCount * prm.batchSize / function.getNumExamples();
            if ((int) Math.floor(passCountFrac) > passCount) {
                // Another full pass through the data has been completed.
                passCount++;
                // Get the value of the function on all the examples.
                value = function.getValue(Utilities.getIndexArray(function.getNumExamples()));
                log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
                log.debug("Learning rate: " + lr);
            }
        }
        
        // Get the final value of the function on all the examples.
        double value = function.getValue(Utilities.getIndexArray(function.getNumExamples()));
        log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
        
        // We don't test for convergence.
        return false;
    }

    /** Gets a batch of indices in the range [0, numExamples). */
    protected int[] getBatch(int numExamples) {
        // Sample the indices with replacement.
        int[] batch = new int[prm.batchSize];
        for (int i=0; i<batch.length; i++) {
            batch[i] = Prng.nextInt(numExamples);
        }
        return batch;
    }
    
    @Override
    public int getBatchSize() {
        return prm.batchSize;
    }
}
