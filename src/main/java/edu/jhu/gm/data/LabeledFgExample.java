package edu.jhu.gm.data;

import java.io.Serializable;
import java.util.List;


import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;

/**
 * Factor graph example. This class facilitates creation of the clamped factor
 * graphs, creation/caching of feature vectors, and storage of the training data
 * assignments to variables.
 * 
 * @author mgormley
 * 
 */
// TODO: rename to CrfExample
public class LabeledFgExample extends UnlabeledFgExample implements FgExample, Serializable {

    private static final long serialVersionUID = 1L;
    
    /** The factor graph with the OBSERVED and PREDICTED variables clamped to their values from the training example. */
    private FactorGraph fgLat;
    /** The variable assignments given in the gold data for all the variables in the factor graph. */
    private VarConfig goldConfig;
    
    // TODO: Figure out how to remove these "initializing" constructors.
    // TODO: Maybe convert to factory methods.
    public LabeledFgExample(FactorGraph fg, VarConfig goldConfig, ObsFeatureExtractor obsFe, FactorTemplateList fts) {
        this(fg, goldConfig);        
        // Initialize the observation function.
        obsFe.init(this, fts);
        // Update the factor templates.
        fts.lookupTemplateIds(this.getFgLatPred());
        fts.getTemplateIds(this.getFgLat());
        fts.getTemplateIds(this.getOriginalFactorGraph());
    }
    public LabeledFgExample(FactorGraph fg, VarConfig goldConfig, FeatureExtractor fe) {
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
    public LabeledFgExample(FactorGraph fg, VarConfig goldConfig) {
        super(fg, goldConfig.getIntersection(VarSet.getVarsOfType(fg.getVars(), VarType.OBSERVED)));
        checkGoldConfig(fg, goldConfig);
        this.fg = fg;
        this.goldConfig = goldConfig;
        fgClampTimer.start();
        
        // Get a copy of the factor graph where the observed and predicted variables are clamped.
        List<Var> predictedVars = VarSet.getVarsOfType(fg.getVars(), VarType.PREDICTED);
        VarConfig predConfig = goldConfig.getIntersection(predictedVars);
        fgLat = fgLatPred.getClamped(predConfig);

        assert (fg.getNumFactors() == fgLat.getNumFactors());
        
        fgClampTimer.stop();
    }

    private static void checkGoldConfig(FactorGraph fg, VarConfig goldConfig) {
        for (Var var : fg.getVars()) {
            // Latent variables don't need to be specified in the gold variable assignment.
            if (var.getType() != VarType.LATENT && goldConfig.getState(var, -1) == -1) {
                throw new IllegalStateException("Vars missing from train configuration: " + var);
            }
        }
    }
    
    /**
     * Gets the factor graph with the OBSERVED and PREDICTED variables clamped
     * to their values from the training example.
     */
    public FactorGraph getFgLat() {
        return fgLat;
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
    
}
