package edu.jhu.gm;

public interface Factor {

    /**
     * Gets a new version of the factor graph where the specified variables are
     * clamped to their given values.
     */
    Factor getClamped(VarConfig clmpVarConfig);

    /** Gets the variables associated with this factor. */
    VarSet getVars();
    
    /** Gets an object which uniquely identifies the feature template for this factor. */
    Object getTemplateKey();
    
    // TODO:
    // void updateFactor(FgModel model);
    
}