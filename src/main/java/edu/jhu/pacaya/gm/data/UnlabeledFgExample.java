package edu.jhu.pacaya.gm.data;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.feat.FactorTemplateList;
import edu.jhu.pacaya.gm.feat.FeatureExtractor;
import edu.jhu.pacaya.gm.feat.ObsFeatureExtractor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.prim.util.Timer;

/**
 * An unlabeled factor graph example. This class only stores the factor graph.
 * 
 * @author mgormley
 */
public class UnlabeledFgExample implements UFgExample, LFgExample, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UnlabeledFgExample.class);
    
    /** The original factor graph. */
    protected FactorGraph fgLatPred;
    /** Whether the original factor graph contains latent variables. */
    protected boolean hasLatentVars;

    public Timer fgClampTimer = new Timer();
    
    // TODO: Figure out how to remove these "initializing" constructors.
    // TODO: Maybe convert to factory methods.
    public UnlabeledFgExample(FactorGraph fg, ObsFeatureExtractor obsFe, FactorTemplateList fts) {
        this(fg);        
        // Initialize the observation function.
        obsFe.init(this, fts);
        // Update the factor templates.
        fts.lookupTemplateIds(this.getFgLatPred());
        fts.getTemplateIds(this.getFgLatPred());
    }
    public UnlabeledFgExample(FactorGraph fg, FeatureExtractor fe) {
        this(fg);        
        // Initialize the feature extractor.
        fe.init(this);        
    }
    
    public UnlabeledFgExample(FactorGraph fg) {
        this.fgLatPred = fg;
        
        // Does this factor graph contain latent variables?
        hasLatentVars = false;
        for (Var var : fgLatPred.getVars()) {
            if (var.getType() == VarType.LATENT) {
                hasLatentVars = true;
            }
        }
        assert (fg.getNumFactors() <= fgLatPred.getNumFactors());
    }

    /** Gets the original input factor graph. */
    public FactorGraph getFgLatPred() {
        return fgLatPred;
    }

    public boolean hasLatentVars() {
        return hasLatentVars;
    }
    
    // Methods of FgExample which throw exceptions if called.
    private static final String DO_NOT_CALL = "Cannot call a labeled factor graph method on an unlabeled factor graph.";
    @Override
    public FactorGraph getFgLat() { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public VarConfig getGoldConfig() { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public VarConfig getGoldConfigPred(int factorId) { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public int getGoldConfigIdxPred(int factorId) { throw new RuntimeException(DO_NOT_CALL); }
    @Override
    public double getWeight() { throw new RuntimeException(DO_NOT_CALL); }

}
