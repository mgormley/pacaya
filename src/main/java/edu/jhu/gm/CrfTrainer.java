package edu.jhu.gm;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.optimize.Function;
import edu.jhu.optimize.Maximizer;
import edu.jhu.util.Utilities;
import edu.jhu.util.vector.IntDoubleEntry;

/**
 * Trainer for a conditional random field (CRF) represented as a factor graph.
 * 
 * @author mgormley
 *
 */
// TODO: This currently does NOT support VarType.LATENT variables. Assert this. Then implement a version that does.
public class CrfTrainer {

    // TODO: Add an option which computes the gradient on only a subset of the
    // variables for use by SGD.
    public static class CrfObjective implements Function {

        private int numParams;
        private FgExamples data;

        public CrfObjective(int numParams, FgExamples data) {
            this.numParams = numParams;
            this.data = data;
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
            double ll = 0.0;
            FgExample ex = data.get(i);
            // Run inference to compute the marginals for each factor.
            FactorGraph fgLat = ex.getFgLat(params);
            FgInferencer inferencer = getInferencer(fgLat);
            inferencer.run();
            
            // Get the "observed" feature counts for this factor, by summing over the latent variables.
            FeatureVector observedFeats = calcExpectedFeatureCounts(fgLat, ex.getFeatCacheLat(), inferencer);                
            ll += observedFeats.dot(params);
            
            // Subtract off the log of the denominator.      
            ll -= inferencer.getPartition();            
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
            // TODO: factor out these BeliefPropagation options.
            BeliefPropagationPrm prm = new BeliefPropagationPrm(fg);
            FgInferencer infForExpFeats = new BeliefPropagation(prm);
            return infForExpFeats;
        }
    }

    public static class CrfTrainerPrm {
        
    }
    
    private FgModel model;
    private FgExamples data;
    
    private Maximizer maximizer;
    private CrfObjective objective;
        
    public CrfTrainer(FgModel model, FgExamples data, Maximizer maximizer) {
        this.model = model;
        this.maximizer = maximizer;
        this.data = data;
        objective = new CrfObjective(model.getNumParams(), data);
    }
    
    // TODO: finish this method.
    public FgModel train() {
        double[] initial = new double[model.getNumParams()];
        // TODO: how to initialize the model parameters?
        double[] params = maximizer.maximize(objective, initial);
        model.setParams(params);
        return model;
    }
    


    
}
