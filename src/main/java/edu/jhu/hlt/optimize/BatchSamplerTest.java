package edu.jhu.hlt.optimize;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.util.Prng;

public class BatchSamplerTest {

    @Before
    public void setUp() {
        Prng.seed(1);
    }
    
    @Test
    public void testWithReplUniformity() {
        // Parameters
        boolean withReplacement = true;
        int batchSize = 15;
        int numExamples = 20;
        int trials = 1000;

        testSampling(withReplacement, batchSize, numExamples, trials);
    }
    

    @Test
    public void testWithoutReplUniformity() {
        // Parameters
        boolean withReplacement = false;
        int batchSize = 15;
        int numExamples = 20;
        int trials = 1000;

        testSampling(withReplacement, batchSize, numExamples, trials);
    }

    @Test
    public void testWithoutReplCorrectness() {
        // Parameters
        boolean withReplacement = false;
        int batchSize = 15;
        int numExamples = 20;
        int trials = 100;

        int[] histogram = new int[numExamples];
        
        int counter = 0;
        BatchSampler samp = new BatchSampler(withReplacement, numExamples, batchSize);
        for (int trial=0; trial<trials; trial++) {
            int[] batch = samp.sampleBatch();
            for (int j=0; j<batch.length; j++) {
                histogram[batch[j]]++;
                counter++;
                
                if (counter == numExamples) {
                    System.out.println(Arrays.toString(histogram));
                    for (int i=0; i<histogram.length; i++) {
                        assertEquals(1, histogram[i]);
                    }
                    Arrays.fill(histogram, 0);
                    counter =0;
                }
            }
        }  
    }
    
    private void testSampling(boolean withReplacement, int batchSize, int numExamples, int trials) {
        int[] histogram = new int[numExamples];
        
        BatchSampler samp = new BatchSampler(withReplacement, numExamples, batchSize);
        for (int trial=0; trial<trials; trial++) {
            int[] batch = samp.sampleBatch();
            for (int j=0; j<batch.length; j++) {
                histogram[batch[j]]++;
            }
        }
        
        for (int i=0; i<histogram.length; i++) {
            double scaledProportion = (double) histogram[i] * numExamples / batchSize / trials;
            assertEquals(1.0, scaledProportion, 1e-1);
        }
    }
    

}
