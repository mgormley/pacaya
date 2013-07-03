package edu.jhu.gm;

import org.apache.log4j.Logger;

import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.optimize.Function;
import edu.jhu.util.Utilities;
import edu.jhu.util.vector.IntDoubleEntry;

// TODO: Add an option which computes the gradient on only a subset of the
// variables for use by SGD.
public class CrfObjective implements Function {
    
    private static final Logger log = Logger.getLogger(CrfObjective.class);
    
    private int numParams;
    private FgExamples data;
    private FgInferencerFactory infFactory;
    
    public CrfObjective(int numParams, FgExamples data, FgInferencerFactory infFactory) {
        this.numParams = numParams;
        this.data = data;
        this.infFactory = infFactory;
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
        double ll = 0.0;
        for (int i=0; i<data.size(); i++) {
            ll += getMarginalLogLikelihoodForExample(i, params);
        }
        return ll;
    }
    
    private double getMarginalLogLikelihoodForExample(int i, double[] params) {
        FgExample ex = data.get(i);

        // TODO: we shouldn't run inference again just to compute this!!
        log.warn("Running inference an extra time to compute marginal likelihood.");
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        FactorGraph fgLat = ex.getFgLat(params);
        FgInferencer infLat = getInferencer(fgLat);
        infLat.run();

        double numerator = infLat.getPartition();

        // "Multiply" in all the fully clamped factors. These are the
        // factors which do not include any latent variables. 
        for (int a=0; a<fgLat.getNumFactors(); a++) {
            Factor f = fgLat.getFactor(a);
            if (f.getVars().size() == 0) {
                FeatureCache cacheLat = ex.getFeatCacheLat();
                numerator += cacheLat.getFeatureVector(a, 0).dot(params);
            }
        }
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        FactorGraph fgLatPred = ex.getFgLatPred(params);
        FgInferencer infLatPred = getInferencer(fgLatPred);
        infLatPred.run();

        double denominator = infLatPred.getPartition();
        
        return numerator - denominator;
    }

    /**
     * Gets the gradient of the conditional log-likelihood.
     * @inheritDoc
     */
    @Override
    public double[] getGradient(double[] params) {
        double[] gradient = new double[params.length];
        for (int i=0; i<data.size(); i++) {
            addGradientForExample(params, i, gradient);
        }
        return gradient;
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(double[] params) {
        FeatureVector obsFeats = new FeatureVector();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            obsFeats.add(calcExpectedFeatureCounts(ex.getFgLat(params), ex.getFeatCacheLat()));
        }
        return obsFeats;
    }
    
    /** Gets the "expected" feature counts. */
    public FeatureVector getExpectedFeatureCounts(double[] params) {
        FeatureVector expFeats = new FeatureVector();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            expFeats.add(calcExpectedFeatureCounts(ex.getFgLatPred(params), ex.getFeatCacheLatPred()));
        }
        return expFeats;
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
        
        // Get the "observed" feature counts for this factor, by summing over the latent variables.
        FeatureVector observedFeats = calcExpectedFeatureCounts(ex.getFgLat(params), ex.getFeatCacheLat());
    
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        FeatureVector expectedFeats = calcExpectedFeatureCounts(ex.getFgLatPred(params), ex.getFeatCacheLatPred());

        // Update the gradient for each feature.
        for (IntDoubleEntry entry : observedFeats.getElementwiseDiff(expectedFeats)) {
            gradient[entry.index()] += entry.get();
        }
    }

    /** Computes the expected feature counts for a factor graph. */
    private FeatureVector calcExpectedFeatureCounts(FactorGraph fg, FeatureCache featCache) {            
        // Run inference to compute the marginals for each factor.
        FgInferencer inferencer = getInferencer(fg);
        inferencer.run();
        return calcExpectedFeatureCounts(fg, featCache, inferencer);
    }
    
    /** 
     * Computes the expected feature counts for a factor graph.
     *  
     * @param factorId The id of the factor.
     * @param featCache The feature cache for the clamped factor graph, on which the inferencer was run.
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @return The feature vector.
     */
    private FeatureVector calcExpectedFeatureCounts(FactorGraph fg, FeatureCache featCache, FgInferencer inferencer) {            
        FeatureVector expectedFeats = new FeatureVector();
        
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {                  
            Factor factorMarginal = inferencer.getMarginalsForFactorId(factorId);
            
            int numConfigs = factorMarginal.getVars().calcNumConfigs();
            if (numConfigs == 0) {
                // If there are no variables in this factor, we still need to get the cached features.
                expectedFeats.add(featCache.getFeatureVector(factorId, 0));
            } else {
                for (int c=0; c<numConfigs; c++) {       
                    // Get the log-probability of the c'th configuration for this factor.
                    // TODO: should factors just store probabilities?
                    double logProb = factorMarginal.getValue(c);
                    double prob = Utilities.exp(logProb);
                    // Get the feature counts when they are clamped to the c'th configuration for this factor.
                    FeatureVector tmpFeats = new FeatureVector(featCache.getFeatureVector(factorId, c));
                    // Scale the feature counts by the marginal probability of the c'th configuration.
                    tmpFeats.scale(prob);
                    expectedFeats.add(tmpFeats);
                }
            }
        }
        return expectedFeats;
    }
    
    /**
     * Gets the number of model parameters.
     */
    @Override
    public int getNumDimensions() {
        return numParams;
    }


    private FgInferencer getInferencer(FactorGraph fg) {
        return infFactory.getInferencer(fg);
    }
}