package edu.jhu.util.tuple;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;


public class PairSamplerTest {

    @Test
    public void testCountMaxPairs() {
        Assert.assertEquals(6, PairSampler.countUnorderedPairs(2, 5, 2, 5));
        Assert.assertEquals(9, PairSampler.countUnorderedPairs(2, 5, 2, 6));
        
        Assert.assertEquals(15, PairSampler.countUnorderedPairs(0, 6, 2, 5));
        Assert.assertEquals(15, PairSampler.countUnorderedPairs(2, 5, 0, 6));
        
        Assert.assertEquals(6, PairSampler.countUnorderedPairs(0, 6, 4, 5));
    }


    @Test
    public void testSampleOrderedPairs() {
        Collection<OrderedPair> samples;
        
        samples = PairSampler.sampleOrderedPairs(0, 100, 0, 1000, 1.0);
        samples = PairSampler.sampleOrderedPairs(0, 1000, 0, 100, 1.0);
        System.out.println(samples.size());
        
        // Test iterative sampling.
        samples = PairSampler.sampleOrderedPairs(0, 312, 100, 212, 0.3);
        System.out.println(samples.size());
        
        // Test hash-set sampling.
        samples = PairSampler.sampleOrderedPairs(0, 312, 0, 312, 0.3);
        System.out.println(samples.size());
    }
    
    @Test
    public void testSampleUnorderedPairs() {
        Collection<UnorderedPair> samples;
        
        samples = PairSampler.sampleUnorderedPairs(0, 100, 0, 1000, 1.0);
        samples = PairSampler.sampleUnorderedPairs(0, 1000, 0, 100, 1.0);
        System.out.println(samples.size());
        
        // Test iterative sampling.
        samples = PairSampler.sampleUnorderedPairs(0, 312, 100, 212, 0.3);
        System.out.println(samples.size());
        
        // Test hash-set sampling.
        samples = PairSampler.sampleUnorderedPairs(0, 312, 0, 312, 0.3);
        System.out.println(samples.size());
    }
    
    // This test is disabled because it takes over 10 seconds.
    //@Test
    public void testSampleOrderedPairsSlow() {
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
    
    // This test is disabled because it takes over 10 seconds.
    //@Test
    public void testSampleUnorderedPairsSlow() {
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
