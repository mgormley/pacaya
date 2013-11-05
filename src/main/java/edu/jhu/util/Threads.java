package edu.jhu.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class Threads {

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

}
