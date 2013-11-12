package edu.jhu.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import static org.junit.Assert.*;

import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.sort.IntSort;
import edu.jhu.util.Threads.TaskFactory;
import edu.jhu.util.collections.Lists;

public class ThreadsTest {

    private static class MockTaskFactory implements TaskFactory<Integer> {

        @Override
        public Callable<Integer> getTask(final int i) {
            return new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return i;
                } 
            };
        }
        
    }
    
    @Test
    public void testSafelyParallize() {
        // This tests that all the correct getTask() calls are made. It does not
        // test that they are parallelized correctly. This would require
        // introspection of the safelyParallizeBatch call.
        ExecutorService pool = Executors.newFixedThreadPool(10);
        int[] batch = new int[]{ 0, 0, 1, 1, 2, 2, 2, 3, 3, 4, 5, 5, 1};
        Prng.seed(System.currentTimeMillis());
        IntArrays.shuffle(batch);
        List<Integer> results = Threads.safelyParallelizeBatch(pool, batch, new MockTaskFactory());
        int[] res = Lists.asArray(results);
        
        IntSort.sortAsc(batch);
        IntSort.sortAsc(res);
        
        assertArrayEquals(batch, res);
    }

    
    
}
