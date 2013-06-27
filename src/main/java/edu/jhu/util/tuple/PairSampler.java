package edu.jhu.util.tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import edu.jhu.util.Prng;

public class PairSampler {

    private PairSampler() {
        // private constructor.
    }

    /**
     * Sample with replacement ordered pairs of integers.
     *  
     * @param minI The minimum value for i (inclusive). 
     * @param maxI The maximum value for i (exclusive).
     * @param minJ The minimum value for j (inclusive).
     * @param maxJ The maximum value for j (exclusive).
     * @param prop The proportion of possible pairs to return.
     * @return A collection of ordered pairs.
     */
    public static Collection<OrderedPair> sampleOrderedPairs(int minI, int maxI, int minJ, int maxJ, double prop) {
        int numI = maxI - minI;
        int numJ = maxJ - minJ;
        
        
        // Count the max number possible:
        long maxPairs = numI * numJ;
        
        Collection<OrderedPair> samples;
        if (maxPairs < 400000000 || prop > 0.1) {
            samples = new ArrayList<OrderedPair>();
    
            for (int i=minI; i<maxI; i++) {
                for (int j=minJ; j<maxJ; j++) {
                    if (prop >= 1.0 || Prng.nextDouble() < prop) {
                        samples.add(new OrderedPair(i, j));
                    }
                }
            }
        } else {
            samples = new HashSet<OrderedPair>();
    
            double numSamples = maxPairs * prop;
            while (samples.size() < numSamples) {
                int i = Prng.nextInt(numI) + minI;
                int j = Prng.nextInt(numJ) + minJ;
                samples.add(new OrderedPair(i, j));
            }
        }
        return samples;
    }

    /**
     * Sample with replacement unordered pairs of integers.
     *  
     * @param minI The minimum value for i (inclusive). 
     * @param maxI The maximum value for i (exclusive).
     * @param minJ The minimum value for j (inclusive).
     * @param maxJ The maximum value for j (exclusive).
     * @param prop The proportion of possible pairs to return.
     * @return A collection of unordered pairs represented as ordered pairs s.t. i <= j. 
     */
    public static Collection<UnorderedPair> sampleUnorderedPairs(int minI, int maxI, int minJ, int maxJ, double prop) {
        int numI = maxI - minI;
        int numJ = maxJ - minJ;
        
        
        // Count the max number possible:
        long maxPairs = PairSampler.countUnorderedPairs(minI, maxI, minJ, maxJ);
        
        Collection<UnorderedPair> samples;
        if (maxPairs < 400000000 || prop > 0.1) {
            samples = new ArrayList<UnorderedPair>();
    
            int min = Math.min(minI, minJ);
            int max = Math.max(maxI, maxJ);
            for (int i=min; i<max; i++) {
                for (int j=i; j<max; j++) {
                    if ((minI <= i && i < maxI && minJ <= j && j < maxJ) ||
                        (minJ <= i && i < maxJ && minI <= j && j < maxI)) {
                        if (prop >= 1.0 || Prng.nextDouble() < prop) {
                            samples.add(new UnorderedPair(i, j));
                        }
                    }
                }
            }
        } else {
            samples = new HashSet<UnorderedPair>();
    
            double numSamples = maxPairs * prop;
            while (samples.size() < numSamples) {
                int i = Prng.nextInt(numI) + minI;
                int j = Prng.nextInt(numJ) + minJ;
                if (i <= j) {
                    // We must reject samples for which j < i, or else we would
                    // be making pairs with i==j half as likely.
                    samples.add(new UnorderedPair(i, j));
                }
            }
        }
        return samples;
    }

    /**
     * Count the number of unordered pairs that satisfy the constraint the
     * constraints: minI <= i < maxI and minJ <= j < maxJ.
     * 
     * Note that since these are unordered pairs. We can think of this as being
     * the count of ordered pairs (i,j) s.t. i<=j and either of the following
     * two conditions holds:
     * 
     * minI <= i < maxI and minJ <= j < maxJ.
     * 
     * minI <= j < maxI and minJ <= i < maxJ.
     * 
     * @param minI The minimum value for i (inclusive). 
     * @param maxI The maximum value for i (exclusive).
     * @param minJ The minimum value for j (inclusive).
     * @param maxJ The maximum value for j (exclusive).
     * @return The number of unordered pairs.
     */
    public static long countUnorderedPairs(int minI, int maxI, int minJ, int maxJ) {
        long maxPairs = 0;
        int min = Math.min(minI, minJ);
        int max = Math.max(maxI, maxJ);
    
        for (int i=min; i<max; i++) {
            for (int j=i; j<max; j++) {
                if ((minI <= i && i < maxI && minJ <= j && j < maxJ) ||
                    (minJ <= i && i < maxJ && minI <= j && j < maxI)) {
                    maxPairs++;
                }
            }
        }
        return maxPairs;
    }
    
}
