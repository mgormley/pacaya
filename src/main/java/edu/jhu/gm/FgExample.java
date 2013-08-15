package edu.jhu.gm;

import java.util.ArrayList;
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
    /** Cache of the features on the observation variables only (i.e. the values of the observation functions). */
    private List<FeatureVector> fvs;

    /**
     * Constructs a train or test example for a Factor Graph.
     * 
     * @param fg The factor graph.
     * @param goldConfig The gold assignment to the variables.
     * @param featExtractor Feature extractor on the observations only (i.e. the
     *            observation function).
     */
    public FgExample(FactorGraph fg, VarConfig goldConfig, CrfFeatureExtractor featExtractor) {
        this(fg, goldConfig, getFvs(fg, featExtractor));
    }
    
    /** Gets the observation feature vector for each factor. */
    private static List<FeatureVector> getFvs(FactorGraph fg, CrfFeatureExtractor featExtractor) {
        List<FeatureVector> fvs = new ArrayList<FeatureVector>(fg.getNumFactors());        
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            if (f instanceof GlobalFactor) {
                fvs.add(null);                
            } else if (f instanceof DenseFactor) {
                fvs.add(featExtractor.calcObsFeatureVector(a));
            } else {
                throw new UnsupportedFactorTypeException(f);
            }
        }
        return fvs;
    }

    /**
     * Constructs a train or test example for a Factor Graph.
     * 
     * @param fg The factor graph.
     * @param goldConfig The gold assignment to the variables.
     * @param fvs The feature vector on the observations only (i.e. the value
     *            of the observation function), indexed by factor id.
     */
    public FgExample(FactorGraph fg, VarConfig goldConfig, List<FeatureVector> fvs) {
        this.fg = fg;
        this.goldConfig = goldConfig;
        this.fvs = fvs;
        
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
            if (f instanceof GlobalFactor) {
                // Currently, global factors do not support features, and
                // therefore have no model parameters.
                continue;
            } else if (f instanceof DenseFactor) {
                
                IntIter iter = null;
                if (fg == this.getFgLat()) {
                    // If this is the numerator then we must clamp the predicted
                    // variables to determine the correct set of model
                    // parameters.
                    VarConfig predVc = this.getGoldConfigPred(a);
                    iter = IndexForVc.getConfigIter(this.getFgLatPred().getFactor(a).getVars(), predVc);
                }
                
                DenseFactor factor = (DenseFactor) f;
                int numConfigs = factor.getVars().calcNumConfigs();
                for (int c=0; c<numConfigs; c++) {
    
                    // The configuration of all the latent/predicted variables,
                    // where the predicted variables (might) have been clamped.
                    int config = (iter != null) ? iter.next() : c;
                    
                    double[] params = model.getParams(model.getTemplates().lookupTemplateId(f), config);
                    FeatureVector fv = getObservationFeatures(a);
                    if (logDomain) {
                        // Set to log of the factor's value.
                        factor.setValue(c, fv.dot(params));
                    } else {
                        factor.setValue(c, Utilities.exp(fv.dot(params)));
                    }
                }
                
            } else {
                throw new RuntimeException("Unsupported factor type: " + f.getClass());
            }
        }
        return fg;
    }

    /** Gets the observation features for the given factor. */
    public FeatureVector getObservationFeatures(int factorId) {
        return fvs.get(factorId);
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
    
}
