package edu.jhu.optimize;

import cern.colt.Arrays;
import edu.jhu.util.Prng;
import edu.jhu.util.Utilities;

public class BatchSampler {

    // Parameters.
    private boolean withReplacement;
    private int numExamples;
    private int batchSize;
    
    // Cache of (shuffled) indices for sampling without replacement.
    private int[] indices;
    // Current index to sample from next.
    private int cur;

    public BatchSampler(boolean withReplacement, int numExamples, int batchSize) {
        this.withReplacement = withReplacement;
        this.numExamples = numExamples;
        this.batchSize = batchSize;
        
        if (!withReplacement) {
            indices = Utilities.getIndexArray(numExamples);
            cur = 0;
        }
    }
    
    public int[] sampleBatch() {
        if (withReplacement) {
            return sampleBatchWithReplacement();
        } else {
            return sampleBatchWithoutReplacement();
        }
    }

    /** Samples a batch of indices in the range [0, numExamples) with replacement. */
    public int[] sampleBatchWithReplacement() {
        // Sample the indices with replacement.
        int[] batch = new int[batchSize];
        for (int i=0; i<batch.length; i++) {
            batch[i] = Prng.nextInt(numExamples);
        }
        return batch;
    }
    
    /** Samples a batch of indices in the range [0, numExamples) without replacement. */
    public int[] sampleBatchWithoutReplacement() {
        int[] batch = new int[batchSize];
        for (int i=0; i<batch.length; i++) {
            if (cur == indices.length) {
                cur = 0;
            }
            if (cur == 0) {
                Utilities.shuffle(indices);
            }
            batch[i] = indices[cur++];
        }
        return batch;
    }
        
}
