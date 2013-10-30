package edu.jhu.gm;

import java.util.List;


/**
 * A collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public interface FgExamples extends Iterable<FgExample> {

    /** Gets the i'th example. */
    public FgExample get(int i);
    
    /** Gets the number of examples. */
    public int size();

    /** Gets the feature templates for these examples. */
    public FeatureTemplateList getTemplates();

    @Deprecated
    public void setSourceSentences(Object sents);
    @Deprecated
    public Object getSourceSentences();
    @Deprecated
    public int getNumFactors();
    @Deprecated
    public int getNumVars();
    
}
