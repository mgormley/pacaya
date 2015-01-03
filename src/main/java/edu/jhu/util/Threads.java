package edu.jhu.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.sort.IntSort;
import edu.jhu.prim.util.Lambda;

public class Threads {

    private static final Logger log = LoggerFactory.getLogger(Threads.class);
    private static final int DONE = -1;

    // NOTE: These should never be set except with a call to initDefaultPool().
    public static ExecutorService defaultPool = null;
    public static int numThreads = 0;
    
    private Threads() { }
    
    public static void initDefaultPool(int numThreads) {
        Threads.defaultPool = Executors.newFixedThreadPool(numThreads);
        Threads.numThreads = numThreads;
    }
    
    public static void shutdownDefaultPool() {
        if (defaultPool != null) {
            shutdownSafelyOrDie(defaultPool);
        }
    }
    
    /* ------------------- Functions using the default pool ----------------- */
    
    public static void forEach(int endIdx, final Lambda.FnIntToVoid fn) {
        forEach(0, endIdx, fn);
    }
    
    public static void forEach(int startIdx, int endIdx, final Lambda.FnIntToVoid fn) {
        List<Callable<Integer>> tasks = new ArrayList<>(endIdx - startIdx);
        for (int i=startIdx; i<endIdx; i++) {
            final int idx = i;
            tasks.add(new Callable<Integer>() {                
                @Override
                public Integer call() {
                    fn.call(idx);
                    return idx;
                }
            });
        }
        invokeAndAwaitAll(defaultPool, tasks);
    }
    
    /* ------------------- Functions using a given pool ----------------- */

    public static <T> List<Future<T>> invokeAndAwaitAll(ExecutorService pool, List<Callable<T>> tasks) {
        List<Future<T>> futures = executeAll(pool, tasks);
        awaitAll(pool, futures);
        return futures;
    }

    public static <T> List<T> getAllResults(ExecutorService pool, ArrayList<Callable<T>> tasks) {
        try {
            List<T> results = new ArrayList<T>();
            List<Future<T>> futures = invokeAndAwaitAll(pool, tasks);
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
            List<Future<T>> futures = invokeAndAwaitAll(pool, tasks);
            for (Future<T> f : futures) {
                results.add(f.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static <T> List<Future<T>> executeAll(ExecutorService pool, Collection<? extends Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = new FutureTask<T>(t);
                futures.add(f);
                pool.execute(f);
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (Future<T> f : futures) {
                    f.cancel(true);
                }
            }
        }
    }
    
    private static <T> void awaitAll(ExecutorService pool, List<Future<T>> futures) {
        int count = 0;
        try {
            boolean[] isDone = new boolean[futures.size()];
            Arrays.fill(isDone, false);
            int i=0;
            while (true) {
                if (count == futures.size()) {
                    break;
                } else if (isDone[i]) {
                    // This task completed. Don't wait for it.
                } else {
                    // Wait for this one to complete.
                    Future<T> f = futures.get(i);
                    try {
                        f.get(100, TimeUnit.MILLISECONDS);
                        count++;
                        isDone[i] = true;
                    } catch (InterruptedException e) {
                        //shutdownSafelyOrDie(pool);
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        //shutdownSafelyOrDie(pool);
                        throw new RuntimeException(e);
                    } catch (TimeoutException e) {
                        // The thread hasn't finished yet.
                    }
                }
                i = (i+1) % futures.size();
            }
        } finally {
            // TODO: For some reason this doesn't work correctly. Instead we
            // have to rely on the "shutdownSafelyOrDie()" calls above.
            if (count != futures.size()) {
                for (Future<T> f : futures) {
                    boolean canceled = f.cancel(true);
                    //log.error("Canceled status: " + canceled);
                }
            }
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

    public static void shutdownSafelyOrDie(ExecutorService pool) {
        log.info("Attempting shutdown of ExecutorService.");
        List<Runnable> tasks = pool.shutdownNow();
        if (tasks.size() != 0) {
            log.error("Tasks were still running when shutdown was called.");
        }
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                log.error("Still awaiting termination after 10 seconds. Exiting now.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while attempting to shutdown. Exiting now.");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
}
