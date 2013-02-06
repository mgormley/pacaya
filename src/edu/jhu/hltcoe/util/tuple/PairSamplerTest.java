package edu.jhu.hltcoe.util.tuple;

import ilog.concert.IloException;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;


public class PairSamplerTest {

    @Test
    public void testCountMaxPairs() throws IloException {
        Assert.assertEquals(6, PairSampler.countUnorderedPairs(2, 5, 2, 5));
        Assert.assertEquals(9, PairSampler.countUnorderedPairs(2, 5, 2, 6));
        
        Assert.assertEquals(15, PairSampler.countUnorderedPairs(0, 6, 2, 5));
        Assert.assertEquals(15, PairSampler.countUnorderedPairs(2, 5, 0, 6));
        
        Assert.assertEquals(6, PairSampler.countUnorderedPairs(0, 6, 4, 5));
    }

    @Test
    public void testSampleOrderedPairs() throws IloException {
        Collection<OrderedPair> samples;
        
        samples = PairSampler.sampleOrderedPairs(0, 100, 0, 1000, 1.0);
        samples = PairSampler.sampleOrderedPairs(0, 1000, 0, 100, 1.0);
        System.out.println(samples.size());
        
        // Test iterative sampling.
        samples = PairSampler.sampleOrderedPairs(0, 31222, 10000, 21222, 0.003);
        System.out.println(samples.size());
        
        // Test hash-set sampling.
        samples = PairSampler.sampleOrderedPairs(0, 31222, 0, 31222, 0.003);
        System.out.println(samples.size());
    }
    
    @Test
    public void testSampleUnorderedPairs() throws IloException {
        Collection<UnorderedPair> samples;
        
        samples = PairSampler.sampleUnorderedPairs(0, 100, 0, 1000, 1.0);
        samples = PairSampler.sampleUnorderedPairs(0, 1000, 0, 100, 1.0);
        System.out.println(samples.size());
        
        // Test iterative sampling.
        samples = PairSampler.sampleUnorderedPairs(0, 31222, 10000, 21222, 0.003);
        System.out.println(samples.size());
        
        // Test hash-set sampling.
        samples = PairSampler.sampleUnorderedPairs(0, 31222, 0, 31222, 0.003);
        System.out.println(samples.size());
    }
    
}
