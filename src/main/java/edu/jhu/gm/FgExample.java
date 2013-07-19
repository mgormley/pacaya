package edu.jhu.gm;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.DepParserRunner;
import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Utilities;

/**
 * Factor graph example. This class facilitates creation of the clamped factor
 * graphs, creation/caching of feature vectors, and storage of the training data
 * assignments to variables.
 * 
 * @author mgormley
 * 
 */
public class FgExample {

    private static final Logger log = Logger.getLogger(FgExample.class);

    /** The original factor graph. */
    private FactorGraph fg;
    /** The factor graph with the OBSERVED variables clamped to their values from the training example. */
    private FactorGraph fgLatPred;
    /** The factor graph with the OBSERVED and PREDICTED variables clamped to their values from the training example. */
    private FactorGraph fgLat;
    /** The feature cache for {@link FgExample#fgLatPred}. */
    private FeatureCache cacheLatPred;
    /** The feature cache for {@link FgExample#fgLat}. */
    private FeatureCache cacheLat;
    /** The feature cache for the fully clamped factor graph. */
    private FeatureCache cacheNone;
    /** Whether the original factor graph contains latent variables. */
    private boolean hasLatentVars;
    /** The variable assignments given in the gold data for all the variables in the factor graph. */
    private VarConfig goldConfig;
    /** The feature extract for the original factor graph. */
    private FeatureExtractor featExtractor;
    
    public FgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor featExtractor) {
        this.fg = fg;
        this.goldConfig = goldConfig;
        this.featExtractor = featExtractor;
        
        // TODO: assert that the training config contains assignments for all the observed and predicted variables.
        
        // Get a copy of the factor graph where the observed variables are clamped.
        List<Var> observedVars = VarSet.getVarsOfType(fg.getVars(), VarType.OBSERVED);
        fgLatPred = fg.getClamped(goldConfig.getIntersection(observedVars));        
        
        // Get a copy of the factor graph where the observed and predicted variables are clamped.
        List<Var> predictedVars = VarSet.getVarsOfType(fg.getVars(), VarType.PREDICTED);
        fgLat = fgLatPred.getClamped(goldConfig.getIntersection(predictedVars));
        
        // Does this factor graph contain latent variables?
        hasLatentVars = fg.getVars().size() - observedVars.size() - predictedVars.size() > 0;

        assert (fg.getNumFactors() == fgLatPred.getNumFactors());
        assert (fg.getNumFactors() == fgLat.getNumFactors());
        
        cacheLat = new FeatureCache(fgLat);
        cacheLatPred = new FeatureCache(fgLatPred);
        
        for (int a=0; a<fg.getNumFactors(); a++) {
            // Get only the predicted and latent variables for this factor.
            VarSet vars = fg.getFactor(a).getVars();
            VarSet varsObs =  VarSet.getVarsOfType(vars, VarType.OBSERVED);
            VarSet varsPred = VarSet.getVarsOfType(vars, VarType.PREDICTED);
            
            // Get the configuration of the specified variables as given in the training data.
            VarConfig obsConfig = goldConfig.getSubset(varsObs);
            VarConfig predConfig = goldConfig.getSubset(varsPred);
            
            if (varsObs.size() != obsConfig.size() || varsPred.size() != predConfig.size()) {
                log.info("varsObs: " + varsObs);
                log.info("varsPred: " + varsPred);
                log.info("obsConfig: " + obsConfig);
                log.info("obsPred: " + predConfig);
                throw new IllegalStateException("Vars missing from train configuration for factor: " + a);
            }
            
            // Cache the features for this factor.
            cacheFeats(fgLat, cacheLat, new VarConfig(obsConfig, predConfig), a);
            cacheFeats(fgLatPred, cacheLatPred, new VarConfig(obsConfig), a);
        }
    }
    
    /**
     * Caches the feature vectors for all variable configurations for a
     * particular factor in a clamped factor graph.
     * 
     * @param fgClamped The clamped factor graph.
     * @param featCache The feature cache for the clamped factor graph.
     * @param clampedVarConfig The configuration of the clamped variables for this factor, whose training data
     *            values are observed.
     * @param factorId The id of the factor.
     */
    private void cacheFeats(FactorGraph fgClamped, FeatureCache featCache,
            VarConfig clampedVarConfig, int factorId) {
        if (fg.getFactor(factorId) instanceof GlobalFactor) {
            // For global factors, we do NOT cache any feature vectors.
            //
            // TODO: to support arbitrary global factors (not just constraints)
            // we should extend global factors to support feature caching of
            // some sort.
            return;
        }
        
        // Get the variables that are marginalized for this factor.
        VarSet vars = fgClamped.getFactor(factorId).getVars();
        int numConfigs = vars.calcNumConfigs();
        if (numConfigs == 0) {
            // If there are no variables in this factor, we still need to cache the features.
            FeatureVector fv = featExtractor.calcFeatureVector(factorId, clampedVarConfig);
            featCache.setFv(factorId, 0, fv);
        } else {
            // For each configuration of the marginalized variables.
            for (int configId = 0; configId < numConfigs; configId++) {
                VarConfig varConfig = vars.getVarConfig(configId);
                // Cache the features.
                VarConfig varConfigFull = new VarConfig(clampedVarConfig, varConfig);
                assert (varConfigFull.size() == varConfig.size() + clampedVarConfig.size());
                FeatureVector fv = featExtractor.calcFeatureVector(factorId, varConfigFull);
                featCache.setFv(factorId, configId, fv);
            }
        }
    }

    /**
     * Gets the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     */
    public FactorGraph getFgLatPred() {
        return fgLatPred;
    }

    /**
     * Gets the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     */
    public FactorGraph getFgLat() {
        return fgLat;
    }
    
    /**
     * Updates the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLatPred(double[] params, boolean logDomain) {
        return getUpdatedFactorGraph(fgLatPred, cacheLatPred, params, logDomain);
    }

    /**
     * Updates the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLat(double[] params, boolean logDomain) {
        return getUpdatedFactorGraph(fgLat, cacheLat, params, logDomain);
    }

    /** Updates the factor graph with the latest parameter vector. 
     * @param logDomain TODO*/
    private static FactorGraph getUpdatedFactorGraph(FactorGraph fg, FeatureCache cache, double[] params, boolean logDomain) {
        for (int a=0; a < fg.getNumFactors(); a++) {
            Factor factor = fg.getFactor(a);
            if (factor instanceof GlobalFactor) {
                // Currently, global factors do not support features, and
                // therefore have no model parameters.
                continue;
            }
            
            int numConfigs = factor.getVars().calcNumConfigs();
            for (int c=0; c<numConfigs; c++) {
                if (logDomain) {
                    // Set to log of the factor's value.
                    factor.setValue(c, cache.getFeatureVector(a, c).dot(params));
                } else {
                    factor.setValue(c, Utilities.exp(cache.getFeatureVector(a, c).dot(params)));
                }
            }
        }
        return fg;
    }

    public boolean hasLatentVars() {
        return hasLatentVars;
    }
    
    /** Gets the feature cache for the factor graph from {@link FgExample#getFgLat()}. */
    public FeatureCache getFeatCacheLat() {
        return cacheLat;
    }

    /** Gets the feature cache for the factor graph from {@link FgExample#getFgLatPred()}. */
    public FeatureCache getFeatCacheLatPred() {
        return cacheLatPred;
    }

    /** Gets the original input factor graph. */
    public FactorGraph getOriginalFactorGraph() {
        return fg;
    }

    /** Gets the gold configuration of the variables. */
    public VarConfig getGoldConfig() {
        return goldConfig;
    }
    
}
