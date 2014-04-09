package edu.jhu.gm.data;

import java.io.Serializable;

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
 * An unlabeled factor graph example. This class only stores the factor graph
 * and the assignment to the observed variables.
 * 
 * @author mgormley
 */
public class UnlabeledFgExample implements FgExample, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(UnlabeledFgExample.class);
    
    /** The original factor graph. */
    protected FactorGraph fg;
    /** The factor graph with the OBSERVED variables clamped to their values from the training example. */
    protected FactorGraph fgLatPred;
    /** Whether the original factor graph contains latent variables. */
    protected boolean hasLatentVars;
    /** The variable assignments for the observed variables only. */
    protected VarConfig obsConfig;
    public Timer fgClampTimer = new Timer();
    
    // TODO: Figure out how to remove these "initializing" constructors.
    // TODO: Maybe convert to factory methods.
    public UnlabeledFgExample(FactorGraph fg, VarConfig obsConfig, ObsFeatureExtractor obsFe, FactorTemplateList fts) {
        this(fg, obsConfig);        
        // Initialize the observation function.
        obsFe.init(this, fts);
        // Update the factor templates.
        fts.lookupTemplateIds(this.getFgLatPred());
        fts.getTemplateIds(this.getOriginalFactorGraph());
    }
    public UnlabeledFgExample(FactorGraph fg, VarConfig obsConfig, FeatureExtractor fe) {
        this(fg, obsConfig);        
        // Initialize the feature extractor.
        fe.init(this);        
    }
    
    public UnlabeledFgExample(FactorGraph fg, VarConfig obsConfig) {
        checkObsConfig(fg, obsConfig);
        this.fg = fg;
        this.obsConfig = obsConfig;
        for(Var v : obsConfig.getVars())
            if(v.getType() != VarType.OBSERVED)
                throw new IllegalArgumentException("obsConfig should only contain observed variables");
        
        fgClampTimer.start();
                        
        // Get a copy of the factor graph where the observed variables are clamped.
        if (obsConfig.size() > 0) {
            fgLatPred = fg.getClamped(obsConfig);
        } else {
            fgLatPred = fg;
        }
        
        // Does this factor graph contain latent variables?
        hasLatentVars = false;
        for (Var var : fgLatPred.getVars()) {
            if (var.getType() == VarType.LATENT) {
                hasLatentVars = true;
            }
        }

        assert (fg.getNumFactors() == fgLatPred.getNumFactors());
        
        fgClampTimer.stop();
    }

    private void checkObsConfig(FactorGraph fg, VarConfig obsConfig) {
        int numObsVarsInFg = 0;
        for (Var var : fg.getVars()) {
            if (var.getType() == VarType.OBSERVED) {
                numObsVarsInFg++;
                if (obsConfig.getState(var, -1) == -1) {
                    throw new IllegalStateException("Vars missing from obs configuration: " + var);
                }
            }
        }
        if (numObsVarsInFg < obsConfig.size()) {
            // TODO: Add this check back in.
            //            VarSet vcVars = obsConfig.getVars();
            //            VarSet fgVars = new VarSet();
            //            fgVars.addAll(fg.getVars());
            //            log.debug("OBSERVED fgVars: " + fgVars.getVarsOfType(VarType.OBSERVED));
            //            throw new RuntimeException("Extra vars in obs configuration:" 
            //                    + " num=" + (obsConfig.size() - numObsVarsInFg) 
            //                    + " vars=" + vcVars.diff(fgVars));
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
     * Updates the factor graph with the OBSERVED variables clamped to their values
     * from the training example.
     * @param params The parameters with which to update.
     * @param logDomain TODO
     */
    public FactorGraph updateFgLatPred(FgModel model, boolean logDomain) {
        return getUpdatedFactorGraph(fgLatPred, model, logDomain);
    }

    /** Updates the factor graph with the latest parameter vector. 
     * @param logDomain TODO*/
    protected FactorGraph getUpdatedFactorGraph(FactorGraph fg, FgModel model, boolean logDomain) {
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

    /** Gets the configuration of the OBSERVED variables. */
    public VarConfig getObsConfig() {
        return obsConfig;
    }
    
    // Methods of FgExample which throw exceptions if called.
    private static final String DO_NOT_CALL = "Cannot call a labeled factor graph method on an unlabeled factor graph.";
    @Override
    public FactorGraph getFgLat() { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public FactorGraph updateFgLat(FgModel model, boolean logDomain) { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public VarConfig getGoldConfig() { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public VarConfig getGoldConfigPred(int factorId) { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public int getGoldConfigIdxPred(int factorId) { throw new RuntimeException(DO_NOT_CALL); }

}