package edu.jhu.gm.train;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.log4j.Logger;

import edu.jhu.gm.model.FgModel;
import edu.jhu.hlt.optimize.function.AbstractDifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
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
public class AvgBatchObjective extends AbstractDifferentiableBatchFunction implements DifferentiableBatchFunction {
    
    /**
     * An objective function for a single example or instance. Calls to these
     * methods are assumed to be threadsafe.
     * 
     * @author mgormley
     */
    public interface ExampleObjective {
        /** Adds the value and gradient for the i'th example. Assumed to be threadsafe. */
        void addValueGradient(FgModel model, int i, MutableValueGradient vg);
        /** Gets the number of examples (i.e. maximum (exclusive) valid value for i in the value / gradient methods. */
        int getNumExamples();
    }
    
    private static final Logger log = Logger.getLogger(AvgBatchObjective.class);
    
    private int numThreads = 1;
    private int numParams;
    private int numExamples;
    private FgModel model;
    private FgModel gradient;
    private ExecutorService pool;
    private ExampleObjective exObj;
        
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
    
    private ValueGradient getValueGradient(IntDoubleVector params, int[] batch, boolean addValue, boolean addGradient) {
        boolean isFullDataset = batch.length == numExamples;

        // Get accumulator for value and gradient. If we don't want to
        // accumulate one or the other, set it to null.
        MutableDouble ll = null;
        FgModel grad = null;
        if (addValue) {
            ll = new MutableDouble(0.0);            
        }
        if (addGradient) {
            this.gradient.zero();
            grad = this.gradient;
        }
        final MutableValueGradient vg = new MutableValueGradient(ll, grad, new MutableDouble(0.0));
        
        model.setParams(params);        
        if (numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                log.trace("Computing value/gradient for example " + i);
                exObj.addValueGradient(model, batch[i], vg);
            }
        } else {
            // Run in parallel.
            TaskFactory<Object> factory = new TaskFactory<Object>() {
                public Callable<Object> getTask(int i) {
                    return new AccumValueGradientOfExample(vg, i);
                }
            };
            Threads.safelyParallelizeBatch(pool, batch, factory);
        }
        
        if (addValue) {
            ll.setValue(ll.doubleValue() / vg.getWeight());
            if (isFullDataset) {
                // Print out the likelihood if we're computing it on the entire dataset.
                log.info("Average objective for full dataset: " + ll);
            }
        }
        if (addGradient) {
            grad.scale(1.0 / vg.getWeight());    
        }

        log.debug("vg.weight = " + vg.getWeight());
        
        return new ValueGradient(addValue ? ll.doubleValue() : Double.NaN, 
                                 addGradient ? grad.getParams() : null);
    }

    private class AccumValueGradientOfExample implements Callable<Object> {

        private MutableValueGradient vg;
        private int i;

        public AccumValueGradientOfExample(MutableValueGradient vg, int i) {
            this.vg = vg;
            this.i = i;
        }

        @Override
        public Object call() {
            MutableValueGradient sparseVg = new MutableValueGradient(null, null, new MutableDouble(0.0));
            synchronized (vg) {
                if (vg.hasValue()) {
                    log.trace("Computing value for example " + i);
                    sparseVg.setValue(new MutableDouble(0.0));    
                }
                if (vg.hasGradient()) {
                    log.trace("Computing gradient for example " + i);
                    FgModel gradient = vg.getGradient();
                    sparseVg.setGradient(gradient.getSparseZeroedCopy());
                }
            }
            
            exObj.addValueGradient(model, i, sparseVg);
            
            synchronized (vg) {         
                if (vg.hasValue()) {
                    vg.addValue(sparseVg.getValue());
                }
                if (vg.hasGradient()) {
                    vg.addGradient(sparseVg.getGradient());
                }
                vg.addWeight(sparseVg.getWeight());
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
    
}