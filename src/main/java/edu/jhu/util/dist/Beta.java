package edu.jhu.util.dist;

import org.apache.commons.math3.distribution.BetaDistribution;

/**
 * Beta distribution.
 * 
 * @author mgormley
 *
 */
public class Beta {
	
    private BetaDistribution beta;

	public Beta(double alpha, double beta) {
	    // TODO: use Prng.
	    this.beta = new BetaDistribution(alpha, beta);
	}
	
	public double nextDouble() {
		return beta.sample();
	}

    public static double nextDouble(double alpha, double beta) {
        Beta b = new Beta(alpha, beta);
        return b.nextDouble();
    }
    
}
