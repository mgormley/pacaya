package edu.jhu.gm;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;
import edu.jhu.optimize.Function;
import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Utilities;

// TODO: Add an option which computes the gradient on only a subset of the
// variables for use by SGD.
public class CrfObjective2 implements Function {
    
    private static final Logger log = Logger.getLogger(CrfObjective2.class);
    
    private int numParams;
    private FgExamples data;
    private FgModel model;
    private FgModel gradient;
    
    // Cached inferencers for each example, indexed by example id.
    private ArrayList<FgInferencer> infLatList;
    private ArrayList<FgInferencer> infLatPredList;

        
    public CrfObjective2(FgModel model, FgExamples data, FgInferencerFactory infFactory) {
        this.numParams = model.getNumParams();
        this.data = data;
        this.model = model;
        this.gradient = new FgModel(model);
        this.gradient.zero();
        
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

        log.debug("Updating model with new parameters");
        model.updateModelFromDoubles(params);
        
        double ll = 0.0;
        for (int i=0; i<data.size(); i++) {
            log.trace("Computing marginal log-likelihood for example " + i);
            ll += getMarginalLogLikelihoodForExample(i);
        }
        log.info("Marginal log-likelihood: " + ll);
        assert ll <= 0 : "Log-likelihood should be <= 0";
        if (ll > 0) {
            throw new IllegalStateException();
        }
        return ll;
    }
    
    private double getMarginalLogLikelihoodForExample(int i) {
        FgExample ex = data.get(i);
        
        // Run inference to compute Z(y,x) by summing over the latent variables w.
        FgInferencer infLat = infLatList.get(i);
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();

        double numerator = infLat.isLogDomain() ? infLat.getPartition() : Utilities.log(infLat.getPartition());

        // "Multiply" in all the fully clamped factors. These are the
        // factors which do not include any latent variables. 
        for (int a=0; a<fgLat.getNumFactors(); a++) {
            Factor f = fgLat.getFactor(a);
            if (f.getVars().size() == 0) {
                int goldConfig = ex.getGoldConfigIdxLatPred(a);
                // TODO: FACTOR THIS OUT. f.potentialFunction(model.getSubmodel(f.getTemplateId()), goldConfig);
               
                double[] params = model.getParams(f.getTemplateId(), goldConfig);
                numerator += ex.getObservationFeatures(a).dot(params);
            }
        }
        infLat.clear();
        
        // Run inference to compute Z(x) by summing over the latent variables w and the predicted variables y.
        FgInferencer infLatPred = infLatPredList.get(i);
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();

        double denominator = infLatPred.isLogDomain() ? infLatPred.getPartition() : Utilities.log(infLatPred.getPartition());

        // "Multiply" in all the fully clamped factors. These are the
        // factors which do not include any latent or predicted variables.
        // This is a bit of an edge case, but we do it anyway.
        for (int a=0; a<fgLatPred.getNumFactors(); a++) {
            Factor f = fgLatPred.getFactor(a);
            if (f.getVars().size() == 0) {
                int goldConfig = ex.getGoldConfigIdxLatPred(a);
                // TODO: FACTOR THIS OUT. f.potentialFunction(model.getSubmodel(f.getTemplateId()), goldConfig);

                double[] params = model.getParams(f.getTemplateId(), goldConfig);
                denominator += ex.getObservationFeatures(a).dot(params);
            }
        }
        infLatPred.clear();
        
        double ll = numerator - denominator;
        assert ll <= 0 : "Log-likelihood should be <= 0: " + ll;
        
        return ll;
    }

    /**
     * Gets the gradient of the conditional log-likelihood.
     * @inheritDoc
     */
    @Override
    public double[] getGradient(double[] params) {
        log.debug("Updating model with new parameters");
        model.updateModelFromDoubles(params);
        
        this.gradient.zero();
        for (int i=0; i<data.size(); i++) {
            log.trace("Computing gradient for example " + i);
            addGradientForExample(i, gradient);
        }        
        double[] g = new double[params.length];
        gradient.updateDoublesFromModel(g);
        return g;
    }
    
    /**
     * Adds the gradient for a particular example to the gradient vector.
     * 
     * @param params The model parameters.
     * @param i The index of the data example.
     * @param gradient The gradient vector to which this example's contribution
     *            is added.
     */
    private void addGradientForExample(int i, FgModel gradient) {
        FgExample ex = data.get(i);
        
        // Compute the "observed" feature counts for this factor, by summing over the latent variables.
        FgInferencer infLat = infLatList.get(i);
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();
        addExpectedFeatureCounts(fgLat, ex, infLat, 1.0, gradient);
        infLat.clear();
        
        // Compute the "expected" feature counts for this factor, by summing over the latent and predicted variables.
        FgInferencer infLatPred = infLatPredList.get(i);
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();
        addExpectedFeatureCounts(fgLatPred, ex, infLatPred, -1.0, gradient);
        infLatPred.clear();
    }

    /** 
     * Computes the expected feature counts for a factor graph, and adds them to the gradient after scaling them.
     * @param ex 
     *  
     * @param factorId The id of the factor.
     * @param featCache The feature cache for the clamped factor graph, on which the inferencer was run.
     * @param inferencer The inferencer for a clamped factor graph, which has already been run.
     * @param multiplier The value which the expected features will be multiplied by.
     * @param gradient The OUTPUT gradient vector to which the scaled expected features will be added.
     */
    private void addExpectedFeatureCounts(FactorGraph fg, FgExample ex, FgInferencer inferencer, double multiplier,
            FgModel gradient) {
        // For each factor...
        for (int factorId=0; factorId<fg.getNumFactors(); factorId++) {     
            Factor f = fg.getFactor(factorId);
            // TODO: FACTOR THIS OUT. f.addGradient(gradient.getSubmodel(f.getTemplateId()), 

            if (f instanceof GlobalFactor) {
                // Special case for global factors.
                continue;
            }
            
            DenseFactor factorMarginal = inferencer.getMarginalsForFactorId(factorId);
            
            int numConfigs = factorMarginal.getVars().calcNumConfigs();
            if (numConfigs == 0) {
                // If there are no variables in this factor, we still need to get the cached features.
                // Update the gradient for each feature.
                FeatureVector fv = ex.getObservationFeatures(factorId);
                int config = ex.getGoldConfigIdxLatPred(factorId);
                for (IntDoubleEntry entry : fv) {
                    gradient.add(f.getTemplateId(), config, entry.index(), multiplier * entry.get());
                }
            } else {
                IntIter iter = null;
                if (fg == ex.getFgLat()) {
                    // If this is the numerator then we must clamp the predicted
                    // variables to determine the correct set of model
                    // parameters.
                    VarConfig predVc = ex.getGoldConfigPred(factorId);
                    iter = IndexForVc.getConfigIter(ex.getFgLatPred().getFactor(factorId).getVars(), predVc);
                }
                
                for (int c=0; c<numConfigs; c++) {       
                    // Get the probability of the c'th configuration for this factor.
                    double prob = factorMarginal.getValue(c);
                    if (inferencer.isLogDomain()) {
                        prob = Utilities.exp(prob);
                    }
                    // Get the feature counts when they are clamped to the c'th configuration for this factor.
                    FeatureVector fv = ex.getObservationFeatures(factorId);

                    // The configuration of all the latent/predicted variables,
                    // where the predicted variables (might) have been clamped.
                    int config = (iter != null) ? iter.next() : c;
                    
                    for (IntDoubleEntry entry : fv) {
                        // Scale the feature counts by the marginal probability of the c'th configuration.
                        // Update the gradient for each feature.
                        gradient.add(f.getTemplateId(), config, entry.index(), multiplier * prob * entry.get());
                    }
                }
                assert(!iter.hasNext());
            }
        }
    }

    /** Gets the "observed" feature counts. */
    public FeatureVector getObservedFeatureCounts(double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = new FgModel(model);
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLat = infLatList.get(i);
            FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
            infLat.run();
            addExpectedFeatureCounts(fgLat, ex, infLat, 1.0, feats);
        }
        double[] f = new double[numParams];
        feats.updateDoublesFromModel(f);
        return new FeatureVector(f);
    }
    
    /** Gets the "expected" feature counts. */
    public FeatureVector getExpectedFeatureCounts(double[] params) {
        model.updateModelFromDoubles(params);
        FgModel feats = new FgModel(model);
        feats.zero();
        for (int i=0; i<data.size(); i++) {
            FgExample ex = data.get(i);
            FgInferencer infLatPred = infLatPredList.get(i);
            FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
            infLatPred.run();
            addExpectedFeatureCounts(fgLatPred, ex, infLatPred, 1.0, feats);
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
    
}