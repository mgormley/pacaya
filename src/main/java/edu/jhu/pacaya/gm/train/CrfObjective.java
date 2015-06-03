package edu.jhu.pacaya.gm.train;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.eval.MseMarginalEvaluator;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.IFgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.prim.util.Timer;

public class CrfObjective implements ExampleObjective {
    
    private static final Logger log = LoggerFactory.getLogger(CrfObjective.class);

    private static final double MAX_LOG_LIKELIHOOD = 1e-10;
    
    private FgExampleList data;
    private FgInferencerFactory infFactory;
    private boolean useMseForValue;
    

    // Timer: clamp the factor graph.
    private Timer fgClampTimer = new Timer();   
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
        this(data, infFactory, false);
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
        try {
            accumWithException(model, i, ac);
        } catch(Throwable t) {
            log.error("Skipping example " + i + " due to throwable: " + t.getMessage());
            t.printStackTrace();
        }
    }
    
    public void accumWithException(FgModel model, int i, Accumulator ac) {
        Timer t0 = new Timer(); t0.start();        
        LFgExample ex = data.get(i);
        Timer t = new Timer();

        // Create a new factor graph with the predicted variables clamped.
        t.reset(); t.start();
        FactorGraph fgLat = getFgLat(ex.getFgLatPred(), ex.getGoldConfig());
        t.stop(); fgClampTimer.add(t);

        // Update the inferences with the current model parameters.
        // (This is usually where feature extraction happens.)
        t.reset(); t.start();
        FactorGraph fgLatPred = ex.getFgLatPred();
        fgLat.updateFromModel(model);
        fgLatPred.updateFromModel(model);
        t.stop(); updTimer.add(t);
        
        // Get the inferencers.
        t.reset(); t.start();
        FgInferencer infLat = infFactory.getInferencer(fgLat);
        FgInferencer infLatPred = infFactory.getInferencer(fgLatPred);
        t.stop(); infTimer.add(t);
        
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
                ac.value += -getMseLoss(infLatPred, ex.getGoldConfig(), ex.getWeight());
            } else {
                // Add the conditional log-likelihood
                ac.value += getValue(fgLat, infLat, fgLatPred, infLatPred, i, ex.getGoldConfig(), ex.getWeight());
            }
            t.stop(); valTimer.add(t);
        }
        if (ac.accumGradient) {
            // Compute the gradient for this example.
            t.reset(); t.start();
            addGradient(fgLat, infLat, fgLatPred, infLatPred, ex.getWeight(), ac.getGradient());
            t.stop(); gradTimer.add(t);
        }
        if (ac.accumWeight) {
            ac.weight += ex.getWeight();
        }
        if (ac.accumLoss) {
            //if (loss != null) { ac.trainLoss += loss.getLoss(i, ex, infLatPred); }
            //ac.trainLoss += getMseLoss(ex, infLatPred);
        }
        t0.stop(); tot.add(t0);
    }
    
    /**
     * Get a copy of the factor graph where the predicted variables are clamped.
     * 
     * @param fgLatPred The original factor graph.
     * @param goldConfig The assignment to the predicted variables.
     * @return The clamped factor graph.
     */
    public static FactorGraph getFgLat(FactorGraph fgLatPred, VarConfig goldConfig) {
        List<Var> predictedVars = VarSet.getVarsOfType(fgLatPred.getVars(), VarType.PREDICTED);
        VarConfig predConfig = goldConfig.getIntersection(predictedVars);
        FactorGraph fgLat = fgLatPred.getClamped(predConfig);
        assert (fgLatPred.getNumFactors() <= fgLat.getNumFactors());
        return fgLat;
    }

    /**
     * Gets the mean-squared error of the i'th example for the given model parameters.
     * 
     * @param infLatPred The inferencer for fgLatPred.
     * @param goldConfig The assignment to the predicted variables.
     * @param weight The weight of this training example.
     * @return The weighted MSE.
     */
    private static double getMseLoss(FgInferencer infLatPred, VarConfig goldConfig, double weight) {
        MseMarginalEvaluator mse = new MseMarginalEvaluator();
        return mse.evaluate(goldConfig, infLatPred) * weight;
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
     * @param goldConfig The assignment to the predicted variables.
     * @param weight The weight of this training example.
     * @return The weighted CLL.
     */      
    public static double getValue(FactorGraph fgLat, FgInferencer infLat, FactorGraph fgLatPred, FgInferencer infLatPred, int i, VarConfig goldConfig, double weight) {        
        // Inference computes Z(y,x) by summing over the latent variables w.
        double numerator = infLat.getLogPartition();
        
        // Inference computes Z(x) by summing over the latent variables w and the predicted variables y.
        double denominator = infLatPred.getLogPartition();

        // "Multiply" in all the fully clamped factors to the numerator. 
        int numFullyClamped = 0;
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            Factor f = fgLatPred.getFactor(a);
            boolean isNumeratorClamped = fgLat.getFactor(a).getVars().size() == 0;
            if (f instanceof GlobalFactor) {
                GlobalFactor gf = (GlobalFactor)f;
                if (isNumeratorClamped) {
                    // These are the factors which do not include any latent variables. 
                    VarConfig facConfig = goldConfig.getIntersection(fgLatPred.getFactor(a).getVars());
                    numerator += gf.getLogUnormalizedScore(facConfig);
                    numFullyClamped++;
                }
            } else {
                if (isNumeratorClamped) {
                    // These are the factors which do not include any latent variables. 
                    int facConfig = goldConfig.getConfigIndexOfSubset(f.getVars());
                    numerator += f.getLogUnormalizedScore(facConfig);
                    numFullyClamped++;
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
        return ll * weight;
    }
    
    /**
     * Adds the gradient of the marginal conditional log-likelihood for a particular example to the gradient vector.
     * @param fgLat The factor graph with the predicted and observed variables clamped. 
     * @param infLat The inferencer for fgLat.
     * @param fgLatPred The factor graph with the observed variables clamped. 
     * @param infLatPred The inferencer for fgLatPred.
     * @param weight The weight of the training example.
     * @param gradient The OUTPUT gradient vector to which this example's contribution
     *            is added.
     */
    private static void addGradient(FactorGraph fgLat, FgInferencer infLat, FactorGraph fgLatPred, FgInferencer infLatPred, double weight, IFgModel gradient) {        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        addExpectedFeatureCounts(fgLat, infLat, 1.0 * weight, gradient);
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        addExpectedFeatureCounts(fgLatPred, infLatPred, -1.0 * weight, gradient);
    }

    /** 
     * Computes the expected feature counts for a factor graph, and adds them to the gradient after scaling them.
     *
     * @param fg The factor graph.
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @param multiplier The value which the expected features will be multiplied by.
     * @param gradient The OUTPUT gradient vector to which the scaled expected features will be added.
     */
    private static void addExpectedFeatureCounts(FactorGraph fg, FgInferencer inferencer, double multiplier, IFgModel gradient) {
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
            FactorGraph fgLat = getFgLat(ex.getFgLatPred(), ex.getGoldConfig());
            fgLat.updateFromModel(model);
            FgInferencer infLat = infFactory.getInferencer(fgLat);
            infLat.run();
            addExpectedFeatureCounts(fgLat, infLat, 1.0 * ex.getWeight(), feats);
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
            FactorGraph fgLatPred = ex.getFgLatPred();
            fgLatPred.updateFromModel(model);
            FgInferencer infLatPred = infFactory.getInferencer(fgLatPred);
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, infLatPred, 1.0 * ex.getWeight(), feats);
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
            log.trace(String.format("Timers avg (ms): clamp=%.1f model=%.1f inf=%.1f val=%.1f grad=%.1f", 
                    fgClampTimer.avgMs(), updTimer.avgMs(), infTimer.avgMs(), valTimer.avgMs(), gradTimer.avgMs()));
        }
        double sum = fgClampTimer.totMs() + updTimer.totMs() + infTimer.totMs() + valTimer.totMs() + gradTimer.totMs();
        double mult = 100.0 / sum;
        log.debug(String.format("Timers: clamp=%.1f model=%.1f%% inf=%.1f%% val=%.1f%% grad=%.1f%% avg(ms)=%.1f max(ms)=%.1f stddev(ms)=%.1f", 
                fgClampTimer.totMs()*mult, updTimer.totMs()*mult, infTimer.totMs()*mult, valTimer.totMs()*mult, gradTimer.totMs()*mult,
                tot.avgMs(), tot.maxSplitMs(), tot.stdDevMs()));
    }
}
