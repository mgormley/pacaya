package edu.jhu.gm.train;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.optimize.BatchFunction;
import edu.jhu.optimize.Function;
import edu.jhu.prim.sort.IntSort;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Threads;
import edu.jhu.util.Threads.TaskFactory;

public class CrfObjective implements Function, BatchFunction {
    
    private static final Logger log = Logger.getLogger(CrfObjective.class);

    private static final double MAX_LOG_LIKELIHOOD = 1e-10;
    
    public static class CrfObjectivePrm {
        public int numThreads = 1;
    }
    
    private CrfObjectivePrm prm;
    private int numParams;
    private FgExampleList data;
    private FgModel model;
    private FgModel gradient;
    private FgInferencerFactory infFactory;
    private ExecutorService pool;
        
    public CrfObjective(CrfObjectivePrm prm, FgModel model, FgExampleList data, FgInferencerFactory infFactory) {
        this.prm = prm;
        this.numParams = model.getNumParams();
        this.data = data;
        this.model = model;
        this.infFactory = infFactory;
        this.gradient = model.getDenseCopy();
        this.gradient.zero();
        this.pool = Executors.newFixedThreadPool(prm.numThreads);
    }
        
    public void setPoint(double[] params) {
        log.trace("Updating model with new parameters");
        model.updateModelFromDoubles(params);
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
    public double getValue() {        
        return getValue(IntSort.getIndexArray(data.size()));
    }

    /**
     * Gets the average marginal conditional log-likelihood computed on a batch.
     * @inheritDoc
     */
    @Override
    public double getValue(int[] batch) {
        // TODO: we shouldn't run inference again just to compute this!!
        boolean isFullDataset = batch.length == data.size();
        double ll = 0.0;
        
        if (prm.numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                ll += getMarginalLogLikelihoodForExample(batch[i]);
            }
        } else {
            // Run in parallel.
            TaskFactory<Double> factory = new TaskFactory<Double>() {
                public Callable<Double> getTask(int i) {
                    return new LogLikelihoodOfExample(i);
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
            log.info("Average marginal log-likelihood: " + ll);
        }
        if ( ll > MAX_LOG_LIKELIHOOD ) {
            String name = isFullDataset ? "data" : "batch";
            log.warn("Log-likelihood for " + name + " should be <= 0: " + ll);
        }
        return ll;
    }
    
    private class LogLikelihoodOfExample implements Callable<Double> {

        private int i;
        
        public LogLikelihoodOfExample(int i) {
            this.i = i;
        }

        @Override
        public Double call() throws Exception {
            return getMarginalLogLikelihoodForExample(i);
        }
        
    }
        
    private double getMarginalLogLikelihoodForExample(int i) {
        FgExample ex = data.get(i);
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        FgInferencer infLat = getInfLat(ex);
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();
        double numerator = infLat.isLogDomain() ? infLat.getPartition() : FastMath.log(infLat.getPartition());
        infLat.clear();
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        FgInferencer infLatPred = getInfLatPred(ex);
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();
        double denominator = infLatPred.isLogDomain() ? infLatPred.getPartition() : FastMath.log(infLatPred.getPartition());        
        infLatPred.clear();
        
        // "Multiply" in all the fully clamped factors to the numerator and denominator. 
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            Factor f = fgLatPred.getFactor(a);

            if (fgLat.getFactor(a).getVars().size() == 0) {
                // These are the factors which do not include any latent variables. 
                int goldConfig = ex.getGoldConfigIdxPred(a);
                numerator += infLat.isLogDomain() ? f.getUnormalizedScore(goldConfig) 
                        : FastMath.log(f.getUnormalizedScore(goldConfig));

                if (fgLatPred.getFactor(a).getVars().size() == 0) {
                    // These are the factors which do not include any latent or predicted variables.
                    // This is a bit of an edge case, but we do it anyway.
                    denominator += infLatPred.isLogDomain() ? f.getUnormalizedScore(goldConfig) 
                            : FastMath.log(f.getUnormalizedScore(goldConfig));
                }
            }
        }
        
        double ll = numerator - denominator;

        if ( ll > MAX_LOG_LIKELIHOOD ) {
            log.warn("Log-likelihood for example should be <= 0: " + ll);
        }
        return ll;
    }

    /**
     * Gets the gradient of the conditional log-likelihood.
     * @inheritDoc
     */
    @Override
    public void getGradient(double[] g) {
        getGradient(IntSort.getIndexArray(data.size()), g);
    }

    /**
     * Gets the gradient of the conditional log-likelihood on a batch of examples.
     * @inheritDoc
     */
    @Override
    public void getGradient(int[] batch, double[] g) {
        this.gradient.zero();   
        if (prm.numThreads == 1) {
            // Run serially.
            for (int i=0; i<batch.length; i++) {
                log.trace("Computing gradient for example " + batch[i]);
                addGradientForExample(batch[i], gradient);
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
        gradient.updateDoublesFromModel(g);
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
            addGradientForExample(i, sparseg);
            synchronized (gradient) {
                gradient.add(sparseg);
            }
            return null;
        }
        
    }
    
    /**
     * Adds the gradient for a particular example to the gradient vector.
     * 
     * @param params The model parameters.
     * @param i The index of the data example.
     * @param gradient The gradient vector to which this example's contribution
     *            is added.
     */
    private void addGradientForExample(int i, IFgModel gradient) {
        FgExample ex = data.get(i);
        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        FgInferencer infLat = getInfLat(ex);
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();
        addExpectedFeatureCounts(fgLat, ex, infLat, data.getTemplates(), 1.0, gradient);
        infLat.clear();
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        FgInferencer infLatPred = getInfLatPred(ex);
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();
        addExpectedFeatureCounts(fgLatPred, ex, infLatPred, data.getTemplates(), -1.0, gradient);
        infLatPred.clear();
    }

    /** 
     * Computes the expected feature counts for a factor graph, and adds them to the gradient after scaling them.
     * @param ex 
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @param fts TODO
     * @param multiplier The value which the expected features will be multiplied by.
     * @param gradient The OUTPUT gradient vector to which the scaled expected features will be added.
     * @param factorId The id of the factor.
     * @param featCache The feature cache for the clamped factor graph, on which the inferencer was run.
     */
    private void addExpectedFeatureCounts(FactorGraph fg, FgExample ex, FgInferencer inferencer, FactorTemplateList fts,
            double multiplier, IFgModel gradient) {
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {     
            Factor f = fg.getFactor(factorId);
            f.addExpectedFeatureCounts(gradient, multiplier, inferencer, factorId);
        }
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = model.getDenseCopy();
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLat = getInfLat(ex);
            FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
            infLat.run();
            addExpectedFeatureCounts(fgLat, ex, infLat, data.getTemplates(), 1.0, feats);
        }
        double[] f = new double[numParams];
        feats.updateDoublesFromModel(f);
        return new FeatureVector(f);
    }
    
    /** Gets the "expected" feature counts. */
    public FeatureVector getExpectedFeatureCounts(double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = model.getDenseCopy();
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLatPred = getInfLatPred(ex);
            FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, ex, infLatPred, data.getTemplates(), 1.0, feats);
        }
        double[] f = new double[numParams];
        feats.updateDoublesFromModel(f);
        return new FeatureVector(f);
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
        return data.size();
    }

    // The old way was to cache all the inferencers. This causes problems
    // because the examples might be recreated on a call to data.get(i).
    private FgInferencer getInfLat(FgExample ex) {
        return infFactory.getInferencer(ex.getFgLat());
    }

    private FgInferencer getInfLatPred(FgExample ex) {
        return infFactory.getInferencer(ex.getFgLatPred());
    }

    public void shutdown() {
        Threads.shutdownSafelyOrDie(pool);
    }
    
}