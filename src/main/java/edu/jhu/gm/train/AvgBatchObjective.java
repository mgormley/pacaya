package edu.jhu.gm.train;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.hlt.optimize.function.DifferentiableBatchFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.sort.IntSort;
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
public class AvgBatchObjective implements DifferentiableFunction, DifferentiableBatchFunction {
    
    /**
     * An objective function for a single example or instance. Calls to these
     * methods are assumed to be threadsafe.
     * 
     * @author mgormley
     */
    public interface ExampleObjective {
        /** Gets the value for the i'th example. Assumed to be threadsafe. */
        double getValue(FgModel model, int i);
        /** Adds the gradient for the i'th example. Assumed to be threadsafe. */
        void addGradient(FgModel model, int i, IFgModel gradient);
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
    
    /**
     * Gets the average marginal conditional log-likelihood of the model for the given model parameters.
     * 
     * We return:
     * <p>
     * \frac{1}{n} \sum_{i=1}^n \log p(y_i | x_i)
     * </p>
     * where:
     * <p>
     * \log p(y|x) = \log \sum_z p(y, z | x)
     * </p>
     * 
     * where y are the predicted variables, x are the observed variables, and z are the latent variables.
     * 
     * @inheritDoc
     */
    @Override
    public double getValue(IntDoubleVector params) {        
        return getValue(params, IntSort.getIndexArray(numExamples));
    }

    /**
     * Gets the average marginal conditional log-likelihood computed on a batch.
     * @inheritDoc
     */
    @Override
    public double getValue(IntDoubleVector params, int[] batch) {
        model.setParams(params);
        // TODO: we shouldn't run inference again just to compute this!!
        boolean isFullDataset = batch.length == numExamples;
        double ll = 0.0;
        
        if (numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                log.trace("Computing value for example " + i);
                ll += exObj.getValue(model, batch[i]);
            }
        } else {
            // Run in parallel.
            TaskFactory<Double> factory = new TaskFactory<Double>() {
                public Callable<Double> getTask(int i) {
                    return new GetValueOfExample(i);
                }
            };
            List<Double> results = Threads.safelyParallelizeBatch(pool, batch, factory);
            for (Double r : results) {
                ll += r;
            }
        }
        
        ll /= batch.length;
        if (isFullDataset) {
            // Print out the likelihood if we're computing it on the entire dataset.
            log.info("Average objective for full dataset: " + ll);
        }
        return ll;
    }
    
    private class GetValueOfExample implements Callable<Double> {

        private int i;
        
        public GetValueOfExample(int i) {
            this.i = i;
        }

        @Override
        public Double call() throws Exception {
            log.trace("Computing value for example " + i);
            return exObj.getValue(model, i);
        }
        
    }

    /**
     * Gets the gradient of the conditional log-likelihood.
     * @inheritDoc
     */
    @Override
    public IntDoubleVector getGradient(IntDoubleVector params) {
        return getGradient(params, IntSort.getIndexArray(numExamples));
    }

    /**
     * Gets the gradient of the conditional log-likelihood on a batch of examples.
     * @inheritDoc
     */
    @Override
    public IntDoubleVector getGradient(IntDoubleVector params, int[] batch) {
        model.setParams(params);
        this.gradient.zero();   
        if (numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                log.trace("Computing gradient for example " + batch[i]);
                exObj.addGradient(model, batch[i], gradient);
            }
        } else {
            // Run in parallel.
            TaskFactory<Object> factory = new TaskFactory<Object>() {
                public Callable<Object> getTask(int i) {
                    return new AddGradientOfExample(gradient, i);
                }
            };
            Threads.safelyParallelizeBatch(pool, batch, factory);
        }     
        this.gradient.scale(1.0 / batch.length);
        return gradient.getParams();
    }

    private class AddGradientOfExample implements Callable<Object> {

        private FgModel gradient;
        private int i;

        public AddGradientOfExample(FgModel gradient, int i) {
            this.gradient = gradient;
            this.i = i;
        }

        @Override
        public Object call() {
            log.trace("Computing gradient for example " + i);
            FgModel sparseg;
            synchronized (gradient) {
                sparseg = gradient.getSparseZeroedCopy();
            }
            exObj.addGradient(model, i, sparseg);
            synchronized (gradient) {
                gradient.add(sparseg);
            }
            return null;
        }
        
    }

    @Override
    public ValueGradient getValueGradient(IntDoubleVector point) {
        return new ValueGradient(getValue(point), getGradient(point));
    }

    @Override
    public ValueGradient getValueGradient(IntDoubleVector point, int[] batch) {
        return new ValueGradient(getValue(point, batch), getGradient(point, batch));
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