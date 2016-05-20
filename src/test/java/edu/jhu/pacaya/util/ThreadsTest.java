package edu.jhu.pacaya.util;

import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import edu.jhu.pacaya.util.Threads.TaskFactory;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.sort.IntSort;
import edu.jhu.prim.util.random.Prng;

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
        int[] res = QLists.asArray(results);
        
        IntSort.sortAsc(batch);
        IntSort.sortAsc(res);
        
        assertArrayEquals(batch, res);
    }

    public static class MockCallable implements Callable<Object> {

        private int i;
        
        public MockCallable(int i) {
            this.i = i;
        }

        @Override
        public Object call() throws Exception {
            // Give everyone a chance to start running.
            Thread.sleep(100);
            if (i == 3) {
                throw new OutOfMemoryError("This is fake");
            } else {
                while (true) {
                    Thread.sleep(1000);
                }
                //    return null;
            }
        }
        
    }
    
    //TODO: This must be run as a Java application. @Test
    public void testSafelyStopOnError() {
        // This tests that a single bad thread throwing an error will cause the complete execution to stop.
        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        for (int i=0; i<10; i++) {
            tasks.add(new MockCallable(i));
        }
        try {
            Threads.invokeAndAwaitAll(pool, tasks);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        new ThreadsTest().testSafelyStopOnError();
    }
    
}
