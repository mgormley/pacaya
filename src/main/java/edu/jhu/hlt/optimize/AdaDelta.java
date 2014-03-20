package edu.jhu.hlt.optimize;

import org.apache.log4j.Logger;

import edu.jhu.hlt.optimize.function.BatchFunction;

/**
 * AdaDelta -- Tweaks the AdaGrad (Duchi et al., 2010) method by adding momentum
 * to the gradient updates. On Neural Net training it seems to be far less
 * sensitive to the learning rate parameters (even though there are two) than
 * SGD or AdaGrad.
 * 
 * Matthew D. Zeiler (2012) "ADADELTA: An Adaptive Learning Rate Method"
 * http://arxiv.org/abs/1212.5701
 * 
 * @author mgormley
 */
public class AdaDelta extends SGD {

    /** Options for this optimizer. */
    public static class AdaDeltaPrm {
        /** The decay rate (rho) for exponential decay averaging. */
        public double decayRate = 0.95;
        /** The amount added (epsilon) to the sum of squares inside the square root. */
        public double constantAddend = Math.pow(Math.E, -6);
        public SGDPrm sgdPrm = new SGDPrm();
    }
    
    private static final Logger log = Logger.getLogger(AdaDelta.class);

    private AdaDeltaPrm prm;
    // Accumulator for gradient.
    private double[] gradAccum;
    // Accumulator for updates.
    private double[] updAccum;
    // Cache of the learning rate for each parameter.
    private double[] lr;
    
    /**
     * Constructs an SGD optimizer.
     */
    public AdaDelta(AdaDeltaPrm prm) {
        super(prm.sgdPrm);
        this.prm = prm;
        if (prm.constantAddend <= 0) {
            throw new IllegalArgumentException("Constant added must be positive: " + prm.constantAddend);
        }
    }

    /**
     * Initializes all the parameters for optimization.
     */
    protected void init(BatchFunction function) {
        super.init(function);
        gradAccum = new double[function.getNumDimensions()];
        lr = new double[function.getNumDimensions()];
        updAccum = new double[function.getNumDimensions()];
    }

    /** A tie-in for subclasses such as AdaGrad. */
    protected void takeNoteOfGradient(double[] gradient) {
        super.takeNoteOfGradient(gradient);        
        for (int i=0; i<gradient.length; i++) {
            gradAccum[i] = prm.decayRate * gradAccum[i] + (1.0 - prm.decayRate) * gradient[i] * gradient[i];
            lr[i] = computeLearningRate(i);
            double update = lr[i] * gradient[i];
            updAccum[i] = prm.decayRate * updAccum[i] + (1.0 - prm.decayRate) * update * update;
            
            assert !Double.isNaN(gradAccum[i]);
            assert !Double.isNaN(lr[i]);
            assert !Double.isNaN(updAccum[i]);
        }
    }
    
    /**
     * The entire point of this method is to carefully compute the following:
     * <p>
     * Math.sqrt(updAccum[i] + prm.constantAddend) / Math.sqrt(gradAccum[i] + prm.constantAddend);
     * </p>
     * without running into boundary cases.
     *  
     * @param i The index of the parameter for which to compute the learning rate.
     * @return The learning rate for that parameter.
     */
    private double computeLearningRate(int i) {
        if (gradAccum[i] < 0) {
            throw new RuntimeException("Gradient accumulator is < 0: " + gradAccum[i]);
        }
        if (updAccum[i] < 0) {
            throw new RuntimeException("Update accumulator is < 0: " + updAccum[i]);
        }

        double learningRate = Math.sqrt(updAccum[i] + prm.constantAddend) / Math.sqrt(gradAccum[i] + prm.constantAddend);        
        assert !Double.isNaN(learningRate);
        // We shouldn't ever worry about infinities because of the constantAdded being > 0.
        assert !Double.isInfinite(learningRate);
        
        return learningRate;
    }
    
    /**
     * Updates the learning rate for the next iteration.
     * @param iterCount The current iteration.
     * @param i The index of the current model parameter. 
     */
    protected double getLearningRate(int iterCount, int i) {
        return lr[i];
    }
    
}
