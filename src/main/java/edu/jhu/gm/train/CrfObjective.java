package edu.jhu.gm.train;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.eval.MseMarginalEvaluator;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Timer;

public class CrfObjective implements ExampleObjective {
    
    private static final Logger log = Logger.getLogger(CrfObjective.class);

    private static final double MAX_LOG_LIKELIHOOD = 1e-10;
    
    private FgExampleList data;
    private FgInferencerFactory infFactory;
    private boolean useMseForValue;
    
    // Timer: update the model.
    private Timer updTimer = new Timer();
    // Timer: run inference.
    private Timer infTimer = new Timer();
    // Timer: compute the log-likelihood.
    private Timer valTimer = new Timer();
    // Timer: compute the gradient.
    private Timer gradTimer = new Timer();
    // Timer: total time for an example.
    private Timer tot = new Timer(); 
    
    public CrfObjective(FgExampleList data, FgInferencerFactory infFactory) {
        this.data = data;
        this.infFactory = infFactory;
    }

    public CrfObjective(FgExampleList data, FgInferencerFactory infFactory, boolean useMseForValue) {
        this.data = data;
        this.infFactory = infFactory;
        this.useMseForValue = useMseForValue;
    }

    /** @inheritDoc */
    // Assumed by caller to be threadsafe.
    @Override
    public void accum(FgModel model, int i, Accumulator ac) {
        Timer t0 = new Timer(); t0.start();        
        LFgExample ex = data.get(i);
        Timer t = new Timer();
        
        // Get the inferencers.
        t.reset(); t.start();
        FgInferencer infLat = infFactory.getInferencer(ex.getFgLat());
        FgInferencer infLatPred = infFactory.getInferencer(ex.getFgLatPred());
        t.stop(); infTimer.add(t);
        
        // Update the inferences with the current model parameters.
        // (This is usually where feature extraction happens.)
        t.reset(); t.start();
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        t.stop(); updTimer.add(t);
        
        t.reset(); t.start();
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        infLat.run();        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        infLatPred.run();
        t.stop(); infTimer.add(t);

        if (ac.accumValue) {
            // Compute the conditional log-likelihood for this example.
            t.reset(); t.start();
            if (useMseForValue) {
                // Add the negative MSE
                ac.value += -getMseLoss(ex, infLatPred);                
            } else {
                // Add the conditional log-likelihood
                ac.value += getValue(ex, fgLat, infLat, fgLatPred, infLatPred, i);
            }
            t.stop(); valTimer.add(t);
        }
        if (ac.accumGradient) {
            // Compute the gradient for this example.
            t.reset(); t.start();
            addGradient(ex, ac.getGradient(), fgLat, infLat, fgLatPred, infLatPred);
            t.stop(); gradTimer.add(t);
        }
        if (ac.accumWeight) {
            ac.weight += ex.getWeight();
        }
        if (ac.accumTrainLoss) {
            //if (loss != null) { ac.trainLoss += loss.getLoss(i, ex, infLatPred); }
            //ac.trainLoss += getMseLoss(ex, infLatPred);
        }
        t0.stop(); tot.add(t0);
    }

    private double getMseLoss(LFgExample ex, FgInferencer infLatPred) {
        MseMarginalEvaluator mse = new MseMarginalEvaluator();
        return mse.evaluate(ex.getGoldConfig(), infLatPred);
    }

    /**
     * Gets the marginal conditional log-likelihood of the i'th example for the given model parameters.
     * 
     * We return:
     * <p>
     * \log p(y|x) = \log \sum_z p(y, z | x)
     * </p>
     * 
     * where y are the predicted variables, x are the observed variables, and z are the latent variables.
     * @param fgLat The factor graph with the predicted and observed variables clamped. 
     * @param infLat The inferencer for fgLat.
     * @param fgLatPred The factor graph with the observed variables clamped. 
     * @param infLatPred The inferencer for fgLatPred.
     * @param i The data example.
     */      
    public double getValue(LFgExample ex, FactorGraph fgLat, FgInferencer infLat, FactorGraph fgLatPred, FgInferencer infLatPred, int i) {        
        // Inference computes Z(y,x) by summing over the latent variables w.
        double numerator = infLat.getLogPartition();
        
        // Inference computes Z(x) by summing over the latent variables w and the predicted variables y.
        double denominator = infLatPred.getLogPartition();

        // "Multiply" in all the fully clamped factors to the numerator and denominator. 
        int numFullyClamped = 0;
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            Factor f = fgLatPred.getFactor(a);
            boolean isNumeratorClamped = fgLat.getFactor(a).getVars().size() == 0;
            boolean isDenominatorClamped = fgLatPred.getFactor(a).getVars().size() == 0;
            if (f instanceof GlobalFactor) {
                GlobalFactor gf = (GlobalFactor)f;
                if (isNumeratorClamped) {
                    // These are the factors which do not include any latent variables. 
                    VarConfig goldConfig = ex.getGoldConfig().getIntersection(fgLatPred.getFactor(a).getVars());
                    numerator += infLat.isLogDomain() ? gf.getUnormalizedScore(goldConfig) 
                            : FastMath.log(gf.getUnormalizedScore(goldConfig));
                    numFullyClamped++;
                    
                    if (isDenominatorClamped) {
                        // These are the factors which do not include any latent or predicted variables.
                        // This is a bit of an edge case, but required for correctness.
                        denominator += infLatPred.isLogDomain() ? gf.getUnormalizedScore(goldConfig) 
                                : FastMath.log(gf.getUnormalizedScore(goldConfig));
                    }
                }
            } else {
                if (isNumeratorClamped) {
                    // These are the factors which do not include any latent variables. 
                    int goldConfig = ex.getGoldConfig().getConfigIndexOfSubset(f.getVars());
                    numerator += infLat.isLogDomain() ? f.getUnormalizedScore(goldConfig) 
                            : FastMath.log(f.getUnormalizedScore(goldConfig));
                    numFullyClamped++;

                    if (isDenominatorClamped) {
                        // These are the factors which do not include any latent or predicted variables.
                        // This is a bit of an edge case, but required for correctness.
                        denominator += infLatPred.isLogDomain() ? f.getUnormalizedScore(goldConfig) 
                                : FastMath.log(f.getUnormalizedScore(goldConfig));
                    }
                }
            }
        }
        
        double ll = numerator - denominator;
        log.trace(String.format("ll=%f numerator=%f denominator=%f", ll, numerator, denominator));
        log.trace(String.format("numFullyClamped=%d numFactors=%d", numFullyClamped, fgLatPred.getFactors().size()));
        
        if (ll > MAX_LOG_LIKELIHOOD) {
            // Note: this can occur if the graph is loopy because the
            // Bethe free energy has miss-estimated -log(Z) or because BP
            // has not yet converged.
            log.warn("Log-likelihood for example "+i+" should be <= 0: " + ll);
        }
        return ll * ex.getWeight();
    }
    
    /**
     * Adds the gradient of the marginal conditional log-likelihood for a particular example to the gradient vector.
     * @param gradient The gradient vector to which this example's contribution
     *            is added.
     * @param fgLat The factor graph with the predicted and observed variables clamped. 
     * @param infLat The inferencer for fgLat.
     * @param fgLatPred The factor graph with the observed variables clamped. 
     * @param infLatPred The inferencer for fgLatPred.
     * @param i The data example.
     */
    public void addGradient(LFgExample ex, IFgModel gradient, FactorGraph fgLat, FgInferencer infLat, FactorGraph fgLatPred, FgInferencer infLatPred) {        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        addExpectedFeatureCounts(fgLat, ex, infLat, 1.0 * ex.getWeight(), gradient);
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        addExpectedFeatureCounts(fgLatPred, ex, infLatPred, -1.0 * ex.getWeight(), gradient);
    }

    /** 
     * Computes the expected feature counts for a factor graph, and adds them to the gradient after scaling them.
     * @param ex 
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @param multiplier The value which the expected features will be multiplied by.
     * @param gradient The OUTPUT gradient vector to which the scaled expected features will be added.
     * @param factorId The id of the factor.
     * @param featCache The feature cache for the clamped factor graph, on which the inferencer was run.
     */
    private void addExpectedFeatureCounts(FactorGraph fg, LFgExample ex, FgInferencer inferencer, double multiplier,
            IFgModel gradient) {
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {     
            Factor f = fg.getFactor(factorId);
            if (f instanceof GlobalFactor) {
                ((GlobalFactor) f).addExpectedFeatureCounts(gradient, multiplier, inferencer, factorId);
            } else {
                VarTensor marg = inferencer.getMarginalsForFactorId(factorId);
                f.addExpectedFeatureCounts(gradient, marg, multiplier);
            }
        }
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(FgModel model, double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = model.getDenseCopy();
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            LFgExample ex = data.get(i);
            FgInferencer infLat = infFactory.getInferencer(ex.getFgLat());
            FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
            infLat.run();
            addExpectedFeatureCounts(fgLat, ex, infLat, 1.0 * ex.getWeight(), feats);
        }
        double[] f = new double[model.getNumParams()];
        feats.updateDoublesFromModel(f);
        return new FeatureVector(f);
    }
    
    /** Gets the "expected" feature counts. */
    public FeatureVector getExpectedFeatureCounts(FgModel model, double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = model.getDenseCopy();
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            LFgExample ex = data.get(i);
            FgInferencer infLatPred = infFactory.getInferencer(ex.getFgLatPred());
            FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, ex, infLatPred, 1.0 * ex.getWeight(), feats);
        }
        double[] f = new double[model.getNumParams()];
        feats.updateDoublesFromModel(f);
        return new FeatureVector(f);
    }

    /** Gets the number of examples in the training dataset. */
    @Override
    public int getNumExamples() {
        return data.size();
    }

    public void report() {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Timers avg (ms): model=%.1f inf=%.1f val=%.1f grad=%.1f", 
                    updTimer.avgMs(), infTimer.avgMs(), valTimer.avgMs(), gradTimer.avgMs()));
        }
        double sum = updTimer.totMs() + infTimer.totMs() + valTimer.totMs() + gradTimer.totMs();
        double mult = 100.0 / sum;
        log.debug(String.format("Timers total%% (ms): model=%.1f inf=%.1f val=%.1f grad=%.1f avg=%.1f max=%.1f stddev=%.1f", 
                updTimer.totMs()*mult, infTimer.totMs()*mult, valTimer.totMs()*mult, gradTimer.totMs()*mult,
                tot.avgMs(), tot.maxSplitMs(), tot.stdDevMs()));
    }
}