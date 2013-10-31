package edu.jhu.optimize;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.util.Prng;
import edu.jhu.util.SafeEquals;

/**
 * AdaGrad (Duchi et al., 2010) -- a first order stochastic gradient method with
 * parameter-specific learning rates.
 * 
 * @author mgormley
 */
public class AdaGrad extends SGD {

    /** Options for this optimizer. */
    public static class AdaGradPrm {
        /** The scaling parameter for the learning rate. */
        public double eta = 0.1;
        /**
         * The amount added (epsilon) to the sum of squares inside the square
         * root. This is to combat the issue of tiny gradients throwing the hole
         * optimization off early on.
         */
        public double constantAddend = 1e-9;
        public SGDPrm sgdPrm = new SGDPrm();
    }
    
    private static final Logger log = Logger.getLogger(AdaGrad.class);

    private AdaGradPrm prm;
    private double[] gradSumSquares;
    
    /**
     * Constructs an SGD optimizer.
     */
    public AdaGrad(AdaGradPrm prm) {
        super(prm.sgdPrm);
        this.prm = prm;
    }

    /**
     * Initializes all the parameters for optimization.
     */
    protected void init(BatchFunction function) {
        super.init(function);
        gradSumSquares = new double[function.getNumDimensions()];
    }

    /** A tie-in for subclasses such as AdaGrad. */
    protected void takeNoteOfGradient(double[] gradient) {
        super.takeNoteOfGradient(gradient);
        for (int i=0; i<gradient.length; i++) {
            gradSumSquares[i] += gradient[i] * gradient[i];
            assert !Double.isNaN(gradSumSquares[i]);
        }
    }
    
    /**
     * Updates the learning rate for the next iteration.
     * @param iterCount The current iteration.
     * @param i The index of the current model parameter. 
     */
    protected double getLearningRate(int iterCount, int i) {
        if (gradSumSquares[i] < 0) {
            throw new RuntimeException("Gradient sum of squares entry is < 0: " + gradSumSquares[i]);
        }
        double learningRate = prm.eta / Math.sqrt(prm.constantAddend + gradSumSquares[i]);
        assert !Double.isNaN(learningRate);
        if (learningRate == Double.POSITIVE_INFINITY) {
            if (gradSumSquares[i] != 0.0) {
                log.warn("Gradient was non-zero but learningRate hit positive infinity: " + gradSumSquares[i]);
            }
            // Just return zero. The gradient is probably 0.0.
            return 0.0;
        }
        return learningRate;
    }
}
