package edu.jhu.pacaya.sch.util.dist;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.util.FastMath;

/**
 * A TruncatedNormal accepts four parameters mu, sigma^2, and lower and upper
 * bounds a and b, for a <= x <= b, the unnormalized pdf at x is the same as a
 * regular Normal, and for x < a or x > b, the unnormalized pdf at x is 0;
 * 
 * to compute the pdf of a TruncatedGaussian, we simple clip and renormalize a
 * regular Normal
 * 
 * see: https://people.sc.fsu.edu/~jburkardt/presentations/truncated_normal.pdf
 * related:
 * https://github.com/mizzao/libmao/blob/master/src/main/java/net/andrewmao/
 * probability/TruncatedNormal.java
 * also see: https://commons.apache.org/proper/commons-math/jacoco/org.apache.commons.math3.distribution/NormalDistribution.java.html
 *
 */
public class TruncatedNormal extends AbstractRealDistribution {

    private static final long serialVersionUID = 1L;
    // mean of untruncated distribution
    private double mu;
    // standard deviation of untruncated distribution
    private double sigma;
    // pdf(x) == 0 for x < lowerBound;
    private double lowerBound;
    // pdf(x) == 0 for x > upperBound;
    private double upperBound;
    // integral of the unnormalized distribution from -inf to a
    private double lowerZ;
    // integral of the unnormalized distribution from b to inf
    private double upperZ;
    // integral of the unnormalized distribution from a to b (1 - (lowerZ +
    // upperZ))
    private double reZ;

    // mean of truncated distribution
    private double mean;
    // variance of truncated distribution
    private double variance;

    // the unnormalized distribution
    private NormalDistribution unnormalized;
    private final static double SQRT2 = FastMath.sqrt(2.0);
    
    public TruncatedNormal(RandomGenerator rng, double mu, double sigma, double lowerBound, double upperBound) {
        super(rng);
        this.mu = mu;
        this.sigma = sigma;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        unnormalized = new NormalDistribution(mu, sigma);
        if (upperBound < lowerBound) {
            throw new IllegalArgumentException("upper bound must be no lower than lower bound");
        }
        lowerZ = unnormalized.cumulativeProbability(lowerBound);
        upperZ = 1 - unnormalized.cumulativeProbability(upperBound);
        reZ = 1 - (lowerZ + upperZ);
        NormalDistribution standardNormal = new NormalDistribution();
        double alpha = (lowerBound - mu) / sigma;
        double beta = (upperBound - mu) / sigma;
        double phiAlpha = standardNormal.density(alpha);
        double phiBeta = standardNormal.density(beta);
        double zPhiAlpha = standardNormal.cumulativeProbability(alpha);
        double zPhiBeta = standardNormal.cumulativeProbability(beta);
        double denom = (zPhiBeta - zPhiAlpha);
        double c = (phiBeta - phiAlpha) / denom;
        mean = mu - sigma * c;
        double d = 1 - c * c;
        if (phiBeta > 0.0) d -= beta * phiBeta / denom;
        if (phiAlpha > 0.0) d += alpha * phiAlpha / denom;
        variance = (sigma * sigma) * d;
        
    }

    /**
     * returns the probability of x falling within the range of a to b
     * under a normal distribution with mean mu and standard deviation sigma
     * if the distribution is truncated below 0 and renormalized
     * 
     *  a and b should both be greater than or equal to 0 but this is not checked
     */
    public static double probabilityTruncZero(double a, double b, double mu, double sigma) {
        // clip at zero
        a = Math.max(a, 0.0);
        b = Math.max(b, 0.0);
        final double denom = sigma * SQRT2;
        final double scaledSDA = (a - mu) / denom; 
        final double scaledSDB = (b - mu) / denom; 
        // compute prob 
        final double probNormTimes2 = Erf.erf(scaledSDA, scaledSDB); 
        // renormalize
        final double scaledSD0 = -mu / denom; 
        final double reZTimes2 = Erf.erfc(scaledSD0);
        return probNormTimes2 / reZTimes2; 
    }

    public static double probabilityNonTrunc(double a, double b, double mu, double sigma) {
        final double denom = sigma * SQRT2;
        final double scaledSDA = (a - mu) / denom; 
        final double scaledSDB = (b - mu) / denom; 
        return 0.5 * Erf.erf(scaledSDA, scaledSDB);
    }

    public static double densityNonTrunc(double x, double mu, double sigma) {
        final double x1 = (x - mu) / sigma;
        final double logStandardDeviationPlusHalfLog2Pi = FastMath.log(sigma) + 0.5 * FastMath.log(2 * FastMath.PI);
        return FastMath.exp(-0.5 * x1 * x1 - logStandardDeviationPlusHalfLog2Pi);
    }

    public static double cumulativeNonTrunc(double x, double mu, double sigma) {
        final double denom = sigma * SQRT2;
        return 0.5 * Erf.erfc((mu - x) / denom);
    }
    
    @Override
    public double cumulativeProbability(double x) {
        if (x < lowerBound) {
            return 0.0;
        } else if (x > upperBound) {
            return 1.0;
        } else {
            // the renormalized clipped cumulative
            return (unnormalized.cumulativeProbability(x) - lowerZ) / reZ;
        }
    }

    @Override
    public double density(double x) {
        if (x < lowerBound || x > upperBound) {
            return 0.0;
        } else {
            return unnormalized.density(x) / reZ;
        }
    }

    @Override
    public double getNumericalMean() {
        return mean;
    }

    @Override
    public double getNumericalVariance() {
        return variance;
    }

    @Override
    public double getSupportLowerBound() {
        return lowerBound;
    }

    @Override
    public double getSupportUpperBound() {
        return upperBound;
    }

    @Override
    public boolean isSupportConnected() {
        return true;
    }

    @Override
    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    @Override
    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public double getNormalMu() {
        return mu;
    }

    public double getNormalSigma() {
        return sigma;
    }

    /**
     * Returns the mean of the normal distribution truncated to 0 for values of x < lowerBound 
     */
    public static double meanTruncLower(double mu, double sigma, double lowerBound) {
        double alpha = (lowerBound - mu) / sigma;
        double phiAlpha = densityNonTrunc(alpha, 0, 1.0);
        double cPhiAlpha = cumulativeNonTrunc(alpha, 0, 1.0);
        return mu + sigma * phiAlpha / (1.0 - cPhiAlpha);
    }

}
