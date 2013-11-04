package edu.jhu.optimize;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.prim.sort.IntSort;
import edu.jhu.util.Timer;

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
        /**
         * The initial learning rate. (i.e. \gamma_0 in where \gamma_t =
         * \frac{\gamma_0}{1 + \gamma_0 \lambda t})
         */
        public double initialLr = 0.1;
        /**
         * Learning rate scaler. (i.e. \lambda in where \gamma_t =
         * \frac{\gamma_0}{1 + \gamma_0 \lambda t})
         * 
         * According to Leon Bottou's (2012) SGD tricks paper, when using an L2
         * regularizer of the form \frac{\lambda}{2} ||w||^2, where w is the
         * weight vector, this should be set to the value \lambda. If the L2
         * regularizer is instead parameterized by the variance of the L2 (i.e.
         * Guassian) prior, then we should set \lambda = 1 / \sigma^2.
         */
        public double lambda = 1.0;
        /** The number of passes over the dataset to perform. */
        public double numPasses = 10;
        /** The batch size to use at each step. */
        public int batchSize = 15;
        /** Whether batches should be sampled with replacement. */
        public boolean withReplacement = false;
        public SGDPrm() { } 
        public SGDPrm(double initialLr, int numPasses, int batchSize) {
            this.initialLr = initialLr;
            this.numPasses = numPasses;
            this.batchSize = batchSize;
        }
    }
    
    private static final Logger log = Logger.getLogger(SGD.class);

    /** The number of gradient steps to run. */   
    private int iterations;
    /** The number of iterations performed thus far. */
    private int iterCount;
    /** The sampler of the indices for each batch. */
    private BatchSampler batchSampler;
   

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
    protected void init(BatchFunction function) {
        int numExamples = function.getNumExamples();

        // Variables
        iterCount = 0;
        batchSampler = new BatchSampler(prm.withReplacement, numExamples, prm.batchSize);
                    
        // Constants
        iterations = (int) Math.ceil((double) prm.numPasses * numExamples / prm.batchSize);
        log.info("Setting number of batch gradient steps: " + iterations);
    }

    /**
     * Updates the learning rate for the next iteration.
     * @param iterCount The current iteration.
     * @param i The index of the current model parameter. 
     */
    protected double getLearningRate(int iterCount, int i) {
        // We use the learning rate suggested in Leon Bottou's (2012) SGD Tricks paper.
        // 
        // \gamma_t = \frac{\gamma_0}{1 + \gamma_0 \lambda t})
        //
        return prm.initialLr / (1 + prm.initialLr * prm.lambda * iterCount);
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
        
        Timer timer = new Timer();
        timer.start();
        int passCount = 0;
        double passCountFrac = 0;
        for (iterCount=0; iterCount < iterations; iterCount++) {
            function.setPoint(point);
            
            int[] batch = batchSampler.sampleBatch();
            
            // Get the current value of the function.
            double value = function.getValue(batch);
            log.trace(String.format("Function value on batch = %g at iteration = %d", value, iterCount));
            
            // Get the gradient of the function.
            Arrays.fill(gradient, 0.0);
            function.getGradient(batch, gradient);
            assert (gradient.length == point.length);                             
            takeNoteOfGradient(gradient);
            
            // Take a step in the direction of the gradient.
            double avgLr = 0.0;
            int numNonZeros = 0;
            for (int i=0; i<point.length; i++) {
                double lr = getLearningRate(iterCount, i);
                if (maximize) {
                    point[i] += lr * gradient[i];
                } else {
                    point[i] -= lr * gradient[i];
                }
                assert !Double.isNaN(point[i]);
                if (gradient[i] != 0.0) {
                    avgLr += lr;
                    numNonZeros++;
                }
            }
            avgLr /= (double) numNonZeros;
            
            // If a full pass through the data has been completed...
            passCountFrac = (double) iterCount * prm.batchSize / function.getNumExamples();
            if ((int) Math.floor(passCountFrac) > passCount || iterCount == iterations - 1) {
                // Another full pass through the data has been completed or we're on the last iteration.
                // Get the value of the function on all the examples.
                value = function.getValue(IntSort.getIndexArray(function.getNumExamples()));
                log.info(String.format("Function value on all examples = %g at iteration = %d on pass = %.2f", value, iterCount, passCountFrac));
                log.debug("Average learning rate: " + avgLr);
                log.debug(String.format("Average time per pass (min): %.2g", timer.totSec() / 60.0 / passCountFrac));
            }
            if ((int) Math.floor(passCountFrac) > passCount) {
                // Another full pass through the data has been completed.
                passCount++;
            }
        }
        
        // We don't test for convergence.
        return false;
    }

    /** A tie-in for subclasses such as AdaGrad. */
    protected void takeNoteOfGradient(double[] gradient) {
        // Do nothing. This is just for subclasses.
    }
    
    @Override
    public int getBatchSize() {
        return prm.batchSize;
    }
}
