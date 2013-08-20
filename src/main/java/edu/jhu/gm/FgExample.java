package edu.jhu.gm;

import java.util.List;

import org.apache.log4j.Logger;

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
// TODO: rename to CrfExample
public class FgExample {

    private static final Logger log = Logger.getLogger(FgExample.class);

    /** The original factor graph. */
    private FactorGraph fg;
    /** The factor graph with the OBSERVED variables clamped to their values from the training example. */
    private FactorGraph fgLatPred;
    /** The factor graph with the OBSERVED and PREDICTED variables clamped to their values from the training example. */
    private FactorGraph fgLat;
    /** Whether the original factor graph contains latent variables. */
    private boolean hasLatentVars;
    /** The variable assignments given in the gold data for all the variables in the factor graph. */
    private VarConfig goldConfig;
    /** The feature extractor. */
    private FeatureExtractor featExtractor;
    
    /**
     * Constructs a train or test example for a Factor Graph, and caches all the features.
     * 
     * @param fg The factor graph.
     * @param goldConfig The gold assignment to the variables.
     * @param featExtractor The feature extractor to be used for this example.
     */
    public FgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor fe) {
        this(fg, goldConfig, fe, true);
    }

    /**
     * Constructs a train or test example for a Factor Graph, and caches all the features.
     * 
     * @param fg The factor graph.
     * @param goldConfig The gold assignment to the variables.
     * @param featExtractor The feature extractor to be used for this example.
     * @param cacheFeats Whether to cache the features, thereby populating the alphabet.
     */
    public FgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor fe, boolean cacheFeats) {
        this.fg = fg;
        this.goldConfig = goldConfig;
        
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
        checkGoldConfig(fg, goldConfig);
        
        this.featExtractor = new FeatureCache(fgLatPred, fe);     
        if (cacheFeats) {
            cacheLatFeats();
            cacheLatPredFeats();
        }
    }

    private static void checkGoldConfig(FactorGraph fg, VarConfig goldConfig) {
        for (int a=0; a<fg.getNumFactors(); a++) {
            // Below, the calls to goldConfig.getSubset() also assert that the training 
            // config contains assignments for all the observed and predicted variables.
            VarSet vars = fg.getFactor(a).getVars();

            // Get only the observed variables for this factor.
            VarSet varsObs =  VarSet.getVarsOfType(vars, VarType.OBSERVED);            
            // Get the configuration of the specified variables as given in the training data.
            VarConfig obsConfig = goldConfig.getSubset(varsObs);
            
            // Get only the observed variables for this factor.
            VarSet varsPred = VarSet.getVarsOfType(vars, VarType.PREDICTED);
            // Get the configuration of the specified variables as given in the training data.
            VarConfig predConfig = goldConfig.getSubset(varsPred);
            
            if (varsObs.size() != obsConfig.size() || varsPred.size() != predConfig.size()) {
                log.info("varsObs: " + varsObs);
                log.info("varsPred: " + varsPred);
                log.info("obsConfig: " + obsConfig);
                log.info("obsPred: " + predConfig);
                throw new IllegalStateException("Vars missing from train configuration for factor: " + a);
            }
        }
    }

    public void cacheLatFeats() {
        getUpdatedFactorGraph(fgLat, new double[]{ }, true);
    }
    
    public void cacheLatPredFeats() {
        getUpdatedFactorGraph(fgLatPred, new double[]{ }, true);
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
        return getUpdatedFactorGraph(fgLatPred, params, logDomain);
    }

    /**
     * Updates the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLat(double[] params, boolean logDomain) {
        return getUpdatedFactorGraph(fgLat, params, logDomain);
    }

    /** Updates the factor graph with the latest parameter vector. 
     * @param logDomain TODO*/
    private FactorGraph getUpdatedFactorGraph(FactorGraph fg, double[] params, boolean logDomain) {
        for (int a=0; a < fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                // Currently, global factors do not support features, and
                // therefore have no model parameters.
                continue;
            } else if (f instanceof DenseFactor) {
                DenseFactor factor = (DenseFactor) f;
                int numConfigs = factor.getVars().calcNumConfigs();

                if (numConfigs == 0) {
                    // HACK: This ensures that if there are no variables in this
                    // factor, we might still create features. This is only
                    // necessary because we also (now) use this method to cache
                    // features.
                    int config = this.getGoldConfigLatPred(a).getConfigIndex();
                    this.getFeatureVector(a, config);
                } else {
                    IntIter iter = null;
                    if (fg == this.getFgLat()) {
                        // If this is the numerator then we must clamp the predicted
                        // variables to determine the correct set of model
                        // parameters.
                        VarConfig predVc = this.getGoldConfigPred(a);
                        iter = IndexForVc.getConfigIter(this.getFgLatPred().getFactor(a).getVars(), predVc);
                    }
                    
                    for (int c=0; c<numConfigs; c++) {
        
                        // The configuration of all the latent/predicted variables,
                        // where the predicted variables (might) have been clamped.
                        int config = (iter != null) ? iter.next() : c;
                        
                        FeatureVector fv = this.getFeatureVector(a, config);
                        if (logDomain) {
                            // Set to log of the factor's value.
                            factor.setValue(c, fv.dot(params));
                        } else {
                            factor.setValue(c, Utilities.exp(fv.dot(params)));
                        }
                    }
                }
                
            } else {
                throw new UnsupportedFactorTypeException(f);
            }
        }
        return fg;
    }

    public boolean hasLatentVars() {
        return hasLatentVars;
    }
    
    /** Gets the original input factor graph. */
    public FactorGraph getOriginalFactorGraph() {
        return fg;
    }

    /** Gets the gold configuration of the variables. */
    public VarConfig getGoldConfig() {
        return goldConfig;
    }

    /** Gets the gold configuration of the latent/predicted variables for the given factor. */
    public VarConfig getGoldConfigLatPred(int factorId) {
        return goldConfig.getIntersection(fgLatPred.getFactor(factorId).getVars());
    }

    /** Gets the gold configuration index of the latent/predicted variables for the given factor. */
    public int getGoldConfigIdxLatPred(int factorId) {
        return goldConfig.getIntersection(fgLatPred.getFactor(factorId).getVars()).getConfigIndex();
    }

    /** Gets the gold configuration of the predicted variables ONLY for the given factor. */ 
    public VarConfig getGoldConfigPred(int factorId) {
        VarSet vars = fgLatPred.getFactor(factorId).getVars();
        return goldConfig.getIntersection(VarSet.getVarsOfType(vars, VarType.PREDICTED));
    }

    /**
     * Gets the specified feature vector.
     * @param factorId The factor id.
     * @param configId The configuration id of the latent and predicted variables for that factor.
     * @return The feature vector.
     */
    public FeatureVector getFeatureVector(int factorId, int configId) {
        return featExtractor.calcFeatureVector(factorId, configId);
    }
    
}
