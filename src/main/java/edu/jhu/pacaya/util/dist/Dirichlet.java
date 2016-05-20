package edu.jhu.pacaya.util.dist;

import java.util.Arrays;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.random.Prng;

public class Dirichlet {

    private static final double EPSILON = Double.MIN_VALUE;

    private double[] alpha;
    
    public Dirichlet(double alphaVal, int k) {
        this.alpha = new double[k];
        Arrays.fill(this.alpha, alphaVal);
    }
    
    public Dirichlet(double[] alpha) {
        this.alpha = alpha;
    }
    
    public double[] draw() {
        return staticDraw(alpha);
    }
    
    public static double[] staticDraw(double[] alpha) {
        double dist[] = new double[alpha.length];
        
        // For each dimension, draw a sample from Gamma(mp_i, 1).
        for (int i = 0; i < dist.length; i++) {
            GammaDistribution gammaDist = new GammaDistribution(rng, alpha[i], 1, 
                    GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
            dist[i] = gammaDist.sample();
            if (dist[i] <= 0) {
                dist[i] = EPSILON;
            }
        }
        
        // Normalize the distribution.
        Multinomials.normalizeProps(dist);

        return dist;
    }
    
    public static RandomGenerator rng = new PrngRandomGenerator();
    
    public static class PrngRandomGenerator implements RandomGenerator {
        
        @Override
        public void setSeed(long seed) {
            throw new RuntimeException("not supported");
        }
        
        @Override
        public void setSeed(int[] seed) {
            throw new RuntimeException("not supported");
        }
        
        @Override
        public void setSeed(int seed) {
            throw new RuntimeException("not supported");
        }
        
        @Override
        public long nextLong() {
            return Prng.nextLong();
        }
        
        @Override
        public int nextInt(int n) {
            return Prng.nextInt(n);
        }
        
        @Override
        public int nextInt() {
            return Prng.nextInt();
        }
        
        @Override
        public double nextGaussian() {
            return Prng.nextDouble();
        }
        
        @Override
        public float nextFloat() {
            return Prng.nextFloat();
        }
        
        @Override
        public double nextDouble() {
            return Prng.nextDouble();
        }
        
        @Override
        public void nextBytes(byte[] arg0) {
            throw new RuntimeException("not supported");
        }
        
        @Override
        public boolean nextBoolean() {
            return Prng.nextBoolean();
        }
    };
    
}
