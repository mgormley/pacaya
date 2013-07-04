package edu.jhu.gm;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.gm.Var.VarType;

/**
 * Factor graph example. This class facilitates creation of the clamped factor
 * graphs, creation/caching of feature vectors, and storage of the training data
 * assignments to variables.
 * 
 * @author mgormley
 * 
 */
public class FgExample {

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
    /** The variable assignments given in the training data for all the variables in the factor graph. */
    private VarConfig trainConfig;
    /** The feature extract for the original factor graph. */
    private FeatureExtractor featExtractor;
    
    public FgExample(FactorGraph fg, VarConfig trainConfig, FeatureExtractor featExtractor) {
        this.fg = fg;
        this.trainConfig = trainConfig;
        this.featExtractor = featExtractor;
        
        // TODO: assert that the training config contains assignments for all the observed and predicted variables.
        
        // Get a copy of the factor graph where the observed variables are clamped.
        List<Var> observedVars = getVarsOfType(fg.getVars(), VarType.OBSERVED);
        fgLatPred = fg.getClamped(trainConfig.getIntersection(observedVars));        
        
        // Get a copy of the factor graph where the observed and predicted variables are clamped.
        List<Var> predictedVars = getVarsOfType(fg.getVars(), VarType.PREDICTED);
        fgLat = fgLatPred.getClamped(trainConfig.getIntersection(predictedVars));
        
        // Does this factor graph contain latent variables?
        hasLatentVars = fg.getVars().size() - observedVars.size() - predictedVars.size() > 0;

        assert (fg.getNumFactors() == fgLatPred.getNumFactors());
        assert (fg.getNumFactors() == fgLat.getNumFactors());
        
        cacheLat = new FeatureCache(fgLat);
        cacheLatPred = new FeatureCache(fgLatPred);
        
        for (int a=0; a<fg.getNumFactors(); a++) {
            // Get only the predicted and latent variables for this factor.
            VarSet vars = fg.getFactor(a).getVars();
            VarSet varsObs =  getVarsOfType(vars, VarType.OBSERVED);
            VarSet varsPred = getVarsOfType(vars, VarType.PREDICTED);
            
            // Get the configuration of the specified variables as given in the training data.
            VarConfig obsConfig = trainConfig.getSubset(varsObs);
            VarConfig predConfig = trainConfig.getSubset(varsPred);
                        
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

    /** Gets the subset of vars with the specified type. */
    private List<Var> getVarsOfType(List<Var> vars, VarType type) {
        ArrayList<Var> subset = new ArrayList<Var>();
        for (Var v : vars) {
            if (v.getType() == type) {
                subset.add(v);
            }
        }
        return subset;      
    }
    
    /** Gets the subset of vars with the specified type. */
    private VarSet getVarsOfType(VarSet vars, VarType type) {
        VarSet subset = new VarSet();
        for (Var v : vars) {
            if (v.getType() == type) {
                subset.add(v);
            }
        }
        return subset;      
    }
    
    /**
     * Gets the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     */
    public FactorGraph getFgLatPred(double[] params) {
        return getUpdatedFactorGraph(fgLatPred, cacheLatPred, params);
    }

    /**
     * Gets the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     * @param params 
     */
    public FactorGraph getFgLat(double[] params) {
        return getUpdatedFactorGraph(fgLat, cacheLat, params);
    }

    /** Updates the factor graph with the latest parameter vector. */
    private static FactorGraph getUpdatedFactorGraph(FactorGraph fg, FeatureCache cache, double[] params) {
        for (int a=0; a < fg.getNumFactors(); a++) {
            Factor factor = fg.getFactor(a);
            int numConfigs = factor.getVars().calcNumConfigs();
            for (int c=0; c<numConfigs; c++) {
                // Set to log of the factor's value.
                factor.setValue(c, cache.getFeatureVector(a, c).dot(params));
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
    
}
