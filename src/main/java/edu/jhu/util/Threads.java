package edu.jhu.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.sort.IntSort;

public class Threads {

    private static final int DONE = -1;

    public static void awaitAll(ArrayList<Future<?>> futures) {
        for (Future<?> f : futures) {
            // Wait for each one to complete.
            try {
                f.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <T> List<T> getAllResults(ExecutorService pool, ArrayList<Callable<T>> tasks) {
        try {
            List<T> results = new ArrayList<T>();
            List<Future<T>> futures = pool.invokeAll(tasks);
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    

    public static <T> void addAllResults(ExecutorService pool, ArrayList<Callable<T>> tasks, List<T> results) {
        try {
            List<Future<T>> futures = pool.invokeAll(tasks);
            for (Future<T> f : futures) {
                results.add(f.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    public interface TaskFactory<T> {
        Callable<T> getTask(int i);
    }

    public static <T> List<T> safelyParallelizeBatch(ExecutorService pool, int[] batch, TaskFactory<T> factory) {
        batch = IntArrays.copyOf(batch);
        ArrayList<T> results = new ArrayList<T>();
        if (!IntSort.isSortedAsc(batch)) {
            Arrays.sort(batch);
        }
        while (IntArrays.count(batch, -1) < batch.length) {
            ArrayList<Callable<T>> tasks = new ArrayList<Callable<T>>();
            for (int i = 0; i < batch.length; i++) {
                if (batch[i] != DONE && (i == batch.length - 1 || batch[i] != batch[i+1])) {
                    Callable<T> task = factory.getTask(batch[i]);
                    tasks.add(task);
                    batch[i] = DONE;
                }
            }
            Threads.addAllResults(pool, tasks, results);
        }
        return results;
    }

}
