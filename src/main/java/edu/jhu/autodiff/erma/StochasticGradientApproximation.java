package edu.jhu.autodiff.erma;

import org.apache.commons.math3.util.FastMath;

import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Prng;
import edu.jhu.util.dist.Gaussian;

public class StochasticGradientApproximation {

    public static IntDoubleVector estimateGradient(Function fn, IntDoubleVector theta0) {
        int numParams = fn.getNumDimensions();
        // The gradient estimate
        IntDoubleVector grad = new IntDoubleDenseVector(numParams);
        // The scaling term
        double c = 0.00001;
        // The dimension
        double p = numParams;
        // The number of samples
        int numSamples = 1000;
        for (int k=0; k<numSamples; k++) {   
            // Get a random direction
            IntDoubleVector e0 = getRandomBernoulliDirection(numParams);
            // Compute ( L(\theta + c * e) - L(\theta - c * e) ) / (2c)
            double scaler = 0;
            {
                // L(\theta + c * e)
                IntDoubleVector e = e0.copy();
                e.scale(c);
                IntDoubleVector theta = theta0.copy();
                theta.add(e);
                scaler += fn.getValue(theta);
            }
            {
                // - L(\theta - c * e)
                IntDoubleVector e = e0.copy();
                e.scale(-c);
                IntDoubleVector theta = theta0.copy();
                theta.add(e);        
                scaler -= fn.getValue(theta);
            }
            scaler /= 2.0 * c;
            for (int i=0; i< numParams; i++) {
                grad.add(i, scaler * 1.0 / e0.get(i));
            }
        }
        grad.scale(1.0 / numSamples);
        return grad;
    }
    
    /* ----------- For random directions ------------ */
    
    public static IntDoubleVector getRandomBernoulliDirection(int p) {
        IntDoubleVector e = new IntDoubleDenseVector(p);
        for (int i=0; i<p; i++) {
            // Bernoulli distribution chooses either positive or negative 1.
            e.set(i, (Prng.nextBoolean()) ? 1 : -1);
        }
        return e;
    }

    public static IntDoubleVector getRandomGaussianDirection(int p) {
        IntDoubleVector e = new IntDoubleDenseVector(p);
        for (int i=0; i<p; i++) {
            e.set(i, Gaussian.nextDouble(0, 1));
        }
        scaleL2NormToValue(e, p);
        return e;
    }

    public static void scaleL2NormToValue(IntDoubleVector e, int desiredL2) {
        double curL2 = getSumOfSquares(e);
        e.scale(FastMath.sqrt(desiredL2 / curL2));
        assert Primitives.equals(getSumOfSquares(e), desiredL2, 1e-13); 
    }

    public static double getSumOfSquares(IntDoubleVector e) {
        double curL2 = 0;
        for (int i=0; i<e.getNumImplicitEntries(); i++) {
            curL2 += e.get(i) * e.get(i);
        }
        return curL2;
    }
    
}
