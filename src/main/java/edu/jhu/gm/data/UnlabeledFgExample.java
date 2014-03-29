package edu.jhu.gm.data;

import java.io.Serializable;

import org.apache.log4j.Logger;

import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.util.Timer;

public class UnlabeledFgExample implements Serializable {

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

    public UnlabeledFgExample(FactorGraph fg, VarConfig obsConfig) {
        checkObsConfig(fg, obsConfig);
        this.fg = fg;
        this.obsConfig = obsConfig;
        
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
        for (Var var : fg.getVars()) {
            if (var.getType() == VarType.OBSERVED) {
                if (obsConfig.getState(var, -1) == -1) {
                    throw new IllegalStateException("Vars missing from train configuration: " + var);
                }
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

}