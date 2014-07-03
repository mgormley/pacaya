package edu.jhu.gm.model;

public interface TemplateFactor extends Factor {

    /** Gets an object which uniquely identifies the feature template for this factor. */
    Object getTemplateKey();
    
    /** Gets the template ID or -1 if not set. */
    int getTemplateId();
    
    /** Sets the template ID. */
    void setTemplateId(int templateId);
    
}
