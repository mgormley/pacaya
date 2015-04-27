package edu.jhu.autodiff;

import org.apache.commons.math3.util.FastMath;

import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.random.Prng;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.dist.Gaussian;

/**
 * Methods for performing stochastic gradient approximation and for doing finite difference testing.
 * 
 * @author mgormley
 */
public class StochasticGradientApproximation {
    
    private StochasticGradientApproximation() { }

    /**
     * Estimates a gradient of a function by an independent finite-difference computation along each
     * dimension of the domain of the function.
     * 
     * @param fn Function on which to approximate the gradient.
     * @param x Point at which to approximate the gradient.
     * @param epsilon The size of the finite difference step.
     * @return The approximate gradient.
     */
    public static IntDoubleVector estimateGradientFd(Function fn, IntDoubleVector x, double epsilon) {
        int numParams = fn.getNumDimensions();
        IntDoubleVector gradFd = new IntDoubleDenseVector(numParams);                      
        for (int j=0; j<numParams; j++) {
            // Test the deriviative d/dx_j(f_i(\vec{x}))
            IntDoubleVector d = new IntDoubleDenseVector(numParams);
            d.set(j, 1);
            double dotFd = getGradDotDirApprox(fn, x, d, epsilon);
            gradFd.set(j, dotFd);
        }
        return gradFd;
    }
    
    /**
     * Estimates a gradient of a function by simultaneous perterbations
     * stochastic approximation (SPSA) (Spall, 1992).
     * 
     * @param fn Function on which to approximate the gradient.
     * @param x Point at which to approximate the gradient.
     * @param numSamples Number of samples to take, which will be averaged
     *            together (typically 1).
     * @return The gradient approximation.
     */
    public static IntDoubleVector estimateGradientSpsa(Function fn, IntDoubleVector x, int numSamples) {
        int numParams = fn.getNumDimensions();
        // The gradient estimate
        IntDoubleVector grad = new IntDoubleDenseVector(numParams);
        for (int k=0; k<numSamples; k++) {   
            // Get a random direction
            IntDoubleVector d = getRandomBernoulliDirection(numParams);
            double scaler = getGradDotDirApprox(fn, x, d);
            for (int i=0; i< numParams; i++) {
                grad.add(i, scaler * 1.0 / d.get(i));
            }
        }
        grad.scale(1.0 / numSamples);
        return grad;
    }

    /**
     * Compute f'(x)^T d = ( L(x + c * d) - L(x - c * d) ) / (2c)
     * 
     * @param fn Function, f.
     * @param x Point at which to evaluate the gradient, x.
     * @param d Random direction, d.
     * @param c Epsilon constant.
     * @return
     */
    public static double getGradDotDirApprox(Function fn, IntDoubleVector x, IntDoubleVector d) {
        return getGradDotDirApprox(fn, x, d, getEpsilon(x, d));
    }

    /**
     * Gets an epsilon constant as advised by Andrei (2009).
     * See also, http://timvieira.github.io/blog/post/2014/02/10/gradient-vector-product/. 
     */
    private static double getEpsilon(IntDoubleVector x, IntDoubleVector d) {
        double machineEpsilon = 2.2204460492503131e-16;
        double xInfNorm = DoubleArrays.infinityNorm(x.toNativeArray());
        double dInfNorm = DoubleArrays.infinityNorm(d.toNativeArray());
        return machineEpsilon * (1.0 + xInfNorm) / dInfNorm;
    }

    /**
     * Compute f'(x)^T d = ( L(x + c * d) - L(x - c * d) ) / (2c)
     * 
     * @param fn Function, f.
     * @param x Point at which to evaluate the gradient, x.
     * @param d Random direction, d.
     * @param c Epsilon constant.
     * @return
     */
    public static double getGradDotDirApprox(Function fn, IntDoubleVector x, IntDoubleVector d, double c) {
        double dot = 0;
        {
            // L(\theta + c * d)
            IntDoubleVector d1 = d.copy();
            d1.scale(c);
            IntDoubleVector x1 = x.copy();
            x1.add(d1);
            dot += fn.getValue(x1);
        }
        {
            // - L(\theta - c * d)
            IntDoubleVector d1 = d.copy();
            d1.scale(-c);
            IntDoubleVector x1 = x.copy();
            x1.add(d1);        
            dot -= fn.getValue(x1);
        }
        dot /= (2.0 * c);
        return dot;
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
        scaleL2NormToValue(e, 1.0);
        return e;
    }

    public static void scaleL2NormToValue(IntDoubleVector d, double desiredL2) {
        double curL2 = getSumOfSquares(d);
        d.scale(FastMath.sqrt(desiredL2 / curL2));
        assert Primitives.equals(getSumOfSquares(d), desiredL2, 1e-13); 
    }

    public static double getSumOfSquares(IntDoubleVector d) {
        double curL2 = 0;
        for (int i=0; i<d.getNumImplicitEntries(); i++) {
            curL2 += d.get(i) * d.get(i);
        }
        return curL2;
    }
    
}
