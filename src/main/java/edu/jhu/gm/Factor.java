package edu.jhu.gm;

import java.io.Serializable;

public interface Factor extends Serializable {
    
    /**
     * Gets a new version of the factor graph where the specified variables are
     * clamped to their given values.
     */
    Factor getClamped(VarConfig clmpVarConfig);

    /** Gets the variables associated with this factor. */
    VarSet getVars();
    
    /** Gets an object which uniquely identifies the feature template for this factor. */
    Object getTemplateKey();
    
    /** Gets the template ID or -1 if not set. */
    int getTemplateId();
    
    /** Sets the template ID. */
    void setTemplateId(int templateId);
    
}