package edu.jhu.gm.train;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.FgModel;
import edu.jhu.hlt.optimize.function.AbstractDifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.NonstationaryFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Threads;
import edu.jhu.util.Threads.TaskFactory;

/**
 * An objective function which is computed as the average objective for a set of
 * examples. The objective is always of the form:
 * 
 * f(\theta) = 1/N * \sum_{i=1}^N g_i(\theta)
 * 
 * where N is the number of examples, f is this objective function, g_i is the
 * objective function for the i^th example.
 * 
 * The gradient and value of the objective function are obtained from the
 * {@link ExampleObjective#getValue} and {@link ExampleObjective#addGradient}
 * methods.
 * 
 * @author mgormley
 */
public class AvgBatchObjective extends AbstractDifferentiableBatchFunction implements DifferentiableBatchFunction,
        NonstationaryFunction {
    
    /**
     * An objective function for a single example or instance. Calls to these
     * methods are assumed to be threadsafe.
     * 
     * @author mgormley
     */
    public interface ExampleObjective {
        /** Adds the value, gradient, and other quantities for the i'th example. Assumed to be threadsafe. */
        void accum(FgModel model, int i, Accumulator vg);
        /** Gets the number of examples (i.e. maximum (exclusive) valid value for i in the value / gradient methods. */
        int getNumExamples();
        void report();
    }
    
    private static final Logger log = Logger.getLogger(AvgBatchObjective.class);
    
    private int numThreads = 1;
    private int numParams;
    private int numExamples;
    private FgModel model;
    private FgModel gradient;
    private ExecutorService pool;
    private ExampleObjective exObj;
    // For nonstationary functions:
    private int curIter;
    private int maxIter;
    
    public AvgBatchObjective(ExampleObjective exObj, FgModel model, int numThreads) {
        this.exObj = exObj;
        this.numThreads = numThreads;
        this.numExamples = exObj.getNumExamples();
        this.numParams = model.getNumParams();
        this.model = model;
        this.gradient = model.getDenseCopy();
        this.gradient.zero();
        this.pool = Executors.newFixedThreadPool(numThreads);
    }

    /** @inheritDoc */
    @Override
    public double getValue(IntDoubleVector params, int[] batch) {
        return getValueGradient(params, batch, true, false).getValue();
    }
    
    /** @inheritDoc */
    @Override
    public IntDoubleVector getGradient(IntDoubleVector params, int[] batch) {
        return getValueGradient(params, batch, false, true).getGradient();
    }
    
    /** @inheritDoc */
    @Override
    public ValueGradient getValueGradient(IntDoubleVector params, int[] batch) {
        return getValueGradient(params, batch, true, true);
    }
    
    private ValueGradient getValueGradient(IntDoubleVector params, int[] batch, final boolean addValue, final boolean addGradient) {
        // Get accumulator for value and gradient.
        final Accumulator ac = new Accumulator();
        ac.accumValue = addValue;
        ac.accumGradient = addGradient; 
        ac.curIter = curIter;
        ac.maxIter = maxIter;
        accum(params, batch, ac);
        return new ValueGradient(ac.accumValue ? ac.value : Double.NaN, 
                ac.accumGradient ? ac.gradient.getParams() : null);
    }

    private void accum(IntDoubleVector params, int[] batch, final Accumulator ac) {
        boolean isFullDataset = (batch.length == numExamples);

        if (isFullDataset) {
            // Include some additional accumulators so that we can report at the end.
            ac.accumValue = true;
            ac.accumTrainLoss = true;
            ac.accumDevLoss = true;
            ac.accumWeight = true;
        }
        if (ac.accumGradient) {
            this.gradient.zero();
            ac.gradient = this.gradient;
        }
        
        model.setParams(params);        
        if (numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                log.trace("Computing value/gradient for example " + i);
                exObj.accum(model, batch[i], ac);
            }
        } else {
            // Run in parallel.
            TaskFactory<Object> factory = new TaskFactory<Object>() {
                public Callable<Object> getTask(int i) {
                    return new AccumValueGradientOfExample(ac, i);
                }
            };
            Threads.safelyParallelizeBatch(pool, batch, factory);
        }
        
        if (ac.accumValue) {
            ac.value /= batch.length;
        }
        if (ac.accumGradient) {
            ac.gradient.scale(1.0 / batch.length);    
        }
        if (isFullDataset) {
            // Print out the likelihood if we're computing it on the entire dataset.
            log.info(String.format("Summary: avg log-likelihood = %.2g train loss = %.2g dev loss = %.2g weight = %.2g",
                    ac.value, ac.trainLoss, ac.devLoss, ac.weight));
            exObj.report();            
        }
    }

    private class AccumValueGradientOfExample implements Callable<Object> {

        private Accumulator ac;
        private int i;

        public AccumValueGradientOfExample(Accumulator vg, int i) {
            this.ac = vg;
            this.i = i;
        }

        @Override
        public Object call() {
            Accumulator sparseAc = new Accumulator();
            sparseAc.setFlagsFromOther(ac);
            synchronized (ac) {
                if (ac.accumValue) {
                    log.trace("Computing value for example " + i);
                }
                if (ac.accumGradient) {
                    log.trace("Computing gradient for example " + i);
                    sparseAc.setGradient(ac.gradient.getSparseZeroedCopy());
                }
            }
            
            exObj.accum(model, i, sparseAc);
            
            synchronized (ac) {   
                ac.addAll(sparseAc);
            }
            return null;
        }
        
    }
    
    /**
     * Gets the number of model parameters.
     */
    @Override
    public int getNumDimensions() {
        return numParams;
    }

    /** Gets the number of examples in the training dataset. */
    @Override
    public int getNumExamples() {
        return numExamples;
    }

    public void shutdown() {
        Threads.shutdownSafelyOrDie(pool);
    }

    @Override
    public void updatateIterAndMax(int curIter, int maxIter) {
        this.curIter = curIter;
        this.maxIter = maxIter;
    }
    
}