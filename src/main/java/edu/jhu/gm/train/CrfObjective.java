package edu.jhu.gm.train;

import org.apache.log4j.Logger;

import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.GlobalFactor;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.train.AvgBatchObjective.ExampleObjective;
import edu.jhu.prim.util.math.FastMath;

public class CrfObjective implements ExampleObjective {
    
    private static final Logger log = Logger.getLogger(CrfObjective.class);

    private static final double MAX_LOG_LIKELIHOOD = 1e-10;
    
    private FgExampleList data;
    private FgInferencerFactory infFactory;
        
    public CrfObjective(FgExampleList data, FgInferencerFactory infFactory) {
        this.data = data;
        this.infFactory = infFactory;
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
     * 
     * @inheritDoc
     */      
    @Override
    public double getValue(FgModel model, int i) {
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
        
        if ( ll > MAX_LOG_LIKELIHOOD ) {
            log.warn("Log-likelihood for example should be <= 0: " + ll);
        }
        return ll;
    }
    
    /**
     * Adds the gradient of the marginal conditional log-likelihood for a particular example to the gradient vector.
     * 
     * @param model The current model parameters.
     * @param i The index of the data example.
     * @param gradient The gradient vector to which this example's contribution
     *            is added.
     */
    @Override
    public void addGradient(FgModel model, int i, IFgModel gradient) {
        FgExample ex = data.get(i);
        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        FgInferencer infLat = getInfLat(ex);
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();
        addExpectedFeatureCounts(fgLat, ex, infLat, 1.0, gradient);
        infLat.clear();
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        FgInferencer infLatPred = getInfLatPred(ex);
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();
        addExpectedFeatureCounts(fgLatPred, ex, infLatPred, -1.0, gradient);
        infLatPred.clear();
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
    private void addExpectedFeatureCounts(FactorGraph fg, FgExample ex, FgInferencer inferencer, double multiplier,
            IFgModel gradient) {
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {     
            Factor f = fg.getFactor(factorId);
            f.addExpectedFeatureCounts(gradient, multiplier, inferencer, factorId);
        }
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(FgModel model, double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = model.getDenseCopy();
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLat = getInfLat(ex);
            FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
            infLat.run();
            addExpectedFeatureCounts(fgLat, ex, infLat, 1.0, feats);
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
            FgExample ex = data.get(i);
            FgInferencer infLatPred = getInfLatPred(ex);
            FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, ex, infLatPred, 1.0, feats);
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

    // The old way was to cache all the inferencers. This causes problems
    // because the examples might be recreated on a call to data.get(i).
    private FgInferencer getInfLat(FgExample ex) {
        return infFactory.getInferencer(ex.getFgLat());
    }

    private FgInferencer getInfLatPred(FgExample ex) {
        return infFactory.getInferencer(ex.getFgLatPred());
    }
    
}