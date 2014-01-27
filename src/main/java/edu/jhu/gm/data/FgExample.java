package edu.jhu.gm.data;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.Timer;

/**
 * Factor graph example. This class facilitates creation of the clamped factor
 * graphs, creation/caching of feature vectors, and storage of the training data
 * assignments to variables.
 * 
 * @author mgormley
 * 
 */
// TODO: rename to CrfExample
public class FgExample implements Serializable {

    private static final long serialVersionUID = 1L;
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

    public Timer fgClampTimer = new Timer(); 
    public Timer featCacheTimer = new Timer(); 
    
    // TODO: Figure out how to remove these "initializing" constructors.
    // TODO: Maybe convert to factor methods.
    public FgExample(FactorGraph fg, VarConfig goldConfig, ObsFeatureExtractor obsFe, FactorTemplateList fts) {
        this(fg, goldConfig);        
        // Initialize the observation function.
        obsFe.init(this, fts);
        // Update the factor templates.
        fts.lookupTemplateIds(this.getFgLatPred());
        fts.getTemplateIds(this.getFgLat());
        fts.getTemplateIds(this.getOriginalFactorGraph());
    }
    public FgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor fe) {
        this(fg, goldConfig);        
        // Initialize the feature extractor.
        fe.init(this);        
    }
    
    /**
     * Constructs a train or test example for a Factor Graph.
     * 
     * @param fg The factor graph.
     * @param goldConfig The gold assignment to the variables.
     */
    public FgExample(FactorGraph fg, VarConfig goldConfig) {
        checkGoldConfig(fg, goldConfig);
        this.fg = fg;
        this.goldConfig = goldConfig;

        fgClampTimer.start();
                        
        // Get a copy of the factor graph where the observed variables are clamped.
        List<Var> observedVars = VarSet.getVarsOfType(fg.getVars(), VarType.OBSERVED);
        if (observedVars.size() > 0) {
            fgLatPred = fg.getClamped(goldConfig.getIntersection(observedVars));
        } else {
            fgLatPred = fg;
        }
        
        // Get a copy of the factor graph where the observed and predicted variables are clamped.
        List<Var> predictedVars = VarSet.getVarsOfType(fg.getVars(), VarType.PREDICTED);
        fgLat = fgLatPred.getClamped(goldConfig.getIntersection(predictedVars));
        
        // Does this factor graph contain latent variables?
        hasLatentVars = fg.getVars().size() - observedVars.size() - predictedVars.size() > 0;

        assert (fg.getNumFactors() == fgLatPred.getNumFactors());
        assert (fg.getNumFactors() == fgLat.getNumFactors());
        
        fgClampTimer.stop();

        // Cache the features in order to ensure we correctly populate the FeatureTemplateList.
        featCacheTimer.start();
        // TODO: is there a way to cache the features at this point. Or should we?
        featCacheTimer.stop();
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
    public FactorGraph updateFgLatPred(FgModel model, boolean logDomain) {
        return getUpdatedFactorGraph(fgLatPred, model, logDomain);
    }

    /**
     * Updates the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLat(FgModel model, boolean logDomain) {
        return getUpdatedFactorGraph(fgLat, model, logDomain);
    }

    /** Updates the factor graph with the latest parameter vector. 
     * @param logDomain TODO*/
    private FactorGraph getUpdatedFactorGraph(FactorGraph fg, FgModel model, boolean logDomain) {
        for (int a=0; a < fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            f.updateFromModel(model, logDomain);
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

    /** Gets the gold configuration of the predicted variables ONLY for the given factor. */ 
    public VarConfig getGoldConfigPred(int factorId) {
        VarSet vars = fg.getFactor(factorId).getVars();
        return goldConfig.getIntersection(VarSet.getVarsOfType(vars, VarType.PREDICTED));
    }
    
    /** Gets the gold configuration index of the predicted variables for the given factor. */
    public int getGoldConfigIdxPred(int factorId) {
        VarSet vars = VarSet.getVarsOfType(fg.getFactor(factorId).getVars(), VarType.PREDICTED);
        return goldConfig.getConfigIndexOfSubset(vars);
    }

    // COMMMENTED OUT OLD CODE:

//  * @param cacheFeats Whether to cache the features, thereby populating the alphabet.
//  */
// public FgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor fe, boolean cacheFeats) {
    
//    public void cacheLatFeats() {
//        getUpdatedFactorGraph(fgLat, new double[]{ }, true);
//    }
//    
//    public void cacheLatPredFeats() {
//        getUpdatedFactorGraph(fgLatPred, new double[]{ }, true);
//    }
    
//    /**
//     * Gets the specified feature vector.
//     * @param factorId The factor id.
//     * @param configId The configuration id of the latent and predicted variables for that factor.
//     * @return The feature vector.
//     */
//    public FeatureVector getFeatureVector(int factorId, int configId) {
//        return featExtractor.calcFeatureVector(factorId, configId);
//    }
    
}
