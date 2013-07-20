package edu.jhu.gm;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.optimize.Function;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Utilities;

// TODO: Add an option which computes the gradient on only a subset of the
// variables for use by SGD.
public class CrfObjective implements Function {
    
    private static final Logger log = Logger.getLogger(CrfObjective.class);
    
    private int numParams;
    private FgExamples data;
    
    // Cached inferencers for each example, indexed by example id.
    private ArrayList<FgInferencer> infLatList;
    private ArrayList<FgInferencer> infLatPredList;
        
    public CrfObjective(int numParams, FgExamples data, FgInferencerFactory infFactory) {
        this.numParams = numParams;
        this.data = data;

        log.debug("Caching inferencers for all examples.");
        // Cache all the inferencers.
        infLatList = new ArrayList<FgInferencer>(data.size());
        infLatPredList = new ArrayList<FgInferencer>(data.size());
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            // Just get the factor graphs, without updating them.
            infLatList.add(infFactory.getInferencer(ex.getFgLat()));
            infLatPredList.add(infFactory.getInferencer(ex.getFgLatPred()));
        }
    }
    
    
    /**
     * Gets the marginal conditional log-likelihood of the model for the given model parameters.
     * 
     * <p>
     * \log p(y|x) = \log \sum_z p(y, z | x)
     * </p>
     * 
     * where y are the predicted variables, x are the observed variables, and z are the latent variables.
     * 
     * @inheritDoc
     */
    @Override
    public double getValue(double[] params) {
        // TODO: we shouldn't run inference again just to compute this!!
        log.warn("Running inference an extra time to compute marginal likelihood.");

        double ll = 0.0;
        for (int i=0; i<data.size(); i++) {
            log.trace("Computing marginal log-likelihood for example " + i);
            ll += getMarginalLogLikelihoodForExample(i, params);
        }
        log.info("Marginal log-likelihood: " + ll);
        assert ll <= 0 : "Log-likelihood should be <= 0";
        if (ll > 0) {
            throw new IllegalStateException();
        }
        return ll;
    }
    
    private double getMarginalLogLikelihoodForExample(int i, double[] params) {
        FgExample ex = data.get(i);
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        FgInferencer infLat = infLatList.get(i);
        FactorGraph fgLat = ex.updateFgLat(params, infLat.isLogDomain());
        infLat.run();

        double numerator = infLat.isLogDomain() ? infLat.getPartition() : Utilities.log(infLat.getPartition());

        // "Multiply" in all the fully clamped factors. These are the
        // factors which do not include any latent variables. 
        for (int a=0; a<fgLat.getNumFactors(); a++) {
            Factor f = fgLat.getFactor(a);
            if (f.getVars().size() == 0) {
                FeatureCache cacheLat = ex.getFeatCacheLat();
                numerator += cacheLat.getFeatureVector(a, 0).dot(params);
            }
        }
        infLat.clear();
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        FgInferencer infLatPred = infLatPredList.get(i);
        FactorGraph fgLatPred = ex.updateFgLatPred(params, infLatPred.isLogDomain());
        infLatPred.run();

        double denominator = infLatPred.isLogDomain() ? infLatPred.getPartition() : Utilities.log(infLatPred.getPartition());

        // TODO: We could multiply in any fully clamped factors in fgLatPred.
        infLatPred.clear();
        
        double ll = numerator - denominator;        
        assert ll <= 0 : "Log-likelihood should be <= 0";
        
        return ll;
    }

    /**
     * Gets the gradient of the conditional log-likelihood.
     * @inheritDoc
     */
    @Override
    public double[] getGradient(double[] params) {
        double[] gradient = new double[params.length];
        for (int i=0; i<data.size(); i++) {
            log.trace("Computing gradient for example " + i);
            addGradientForExample(params, i, gradient);
        }
        return gradient;
    }
    
    /**
     * Adds the gradient for a particular example to the gradient vector.
     * 
     * @param params The model parameters.
     * @param i The index of the data example.
     * @param gradient The gradient vector to which this example's contribution is added.
     */
    private void addGradientForExample(double[] params, int i,
            double[] gradient) {
        FgExample ex = data.get(i);
        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        FgInferencer infLat = infLatList.get(i);
        FactorGraph fgLat = ex.updateFgLat(params, infLat.isLogDomain());
        infLat.run();
        addExpectedFeatureCounts(fgLat, ex.getFeatCacheLat(), infLat, 1.0, gradient);
        infLat.clear();
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        FgInferencer infLatPred = infLatPredList.get(i);
        FactorGraph fgLatPred = ex.updateFgLatPred(params, infLatPred.isLogDomain());
        infLatPred.run();
        addExpectedFeatureCounts(fgLatPred, ex.getFeatCacheLatPred(), infLatPred, -1.0, gradient);
        infLatPred.clear();
    }

    /** 
     * Computes the expected feature counts for a factor graph, and adds them to the gradient after scaling them.
     *  
     * @param factorId The id of the factor.
     * @param featCache The feature cache for the clamped factor graph, on which the inferencer was run.
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @param multiplier The value which the expected features will be multiplied by.
     * @param gradient The OUTPUT gradient vector to which the scaled expected features will be added.
     */
    private void addExpectedFeatureCounts(FactorGraph fg, FeatureCache featCache, FgInferencer inferencer, double multiplier,
            double[] gradient) {
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {     
            if (fg.getFactor(factorId) instanceof GlobalFactor) {
                // Special case for global factors.
                continue;
            }
            
            DenseFactor factorMarginal = inferencer.getMarginalsForFactorId(factorId);
            
            int numConfigs = factorMarginal.getVars().calcNumConfigs();
            if (numConfigs == 0) {
                // If there are no variables in this factor, we still need to get the cached features.
                // Update the gradient for each feature.
                for (IntDoubleEntry entry : featCache.getFeatureVector(factorId, 0)) {
                    gradient[entry.index()] += multiplier * entry.get();
                }
            } else {
                for (int c=0; c<numConfigs; c++) {       
                    // Get the probability of the c'th configuration for this factor.
                    double prob = factorMarginal.getValue(c);
                    if (inferencer.isLogDomain()) {
                        prob = Utilities.exp(prob);
                    }
                    // Get the feature counts when they are clamped to the c'th configuration for this factor.
                    for (IntDoubleEntry entry : featCache.getFeatureVector(factorId, c)) {
                        // Scale the feature counts by the marginal probability of the c'th configuration.
                        // Update the gradient for each feature.
                        gradient[entry.index()] += multiplier * prob * entry.get();
                    }
                }
            }
        }
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(double[] params) {
        double[] feats = new double[numParams];
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLat = infLatList.get(i);
            FactorGraph fgLat = ex.updateFgLat(params, infLat.isLogDomain());
            infLat.run();
            addExpectedFeatureCounts(fgLat, ex.getFeatCacheLat(), infLat, 1.0, feats);
        }
        return new FeatureVector(feats);
    }
    
    /** Gets the "expected" feature counts. */
    public FeatureVector getExpectedFeatureCounts(double[] params) {
        double[] feats = new double[numParams];
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLatPred = infLatPredList.get(i);
            FactorGraph fgLatPred = ex.updateFgLatPred(params, infLatPred.isLogDomain());
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, ex.getFeatCacheLatPred(), infLatPred, 1.0, feats);
        }
        return new FeatureVector(feats);
    }
    
    /**
     * Gets the number of model parameters.
     */
    @Override
    public int getNumDimensions() {
        return numParams;
    }
    
}