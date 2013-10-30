package edu.jhu.gm.data;

import edu.jhu.gm.feat.FeatureTemplateList;

/**
 * A factory of instances for a graphical model represented as factor
 * graphs.
 * 
 * @author mgormley
 * 
 */
public interface FgExampleFactory {

    /** Gets the i'th example. */
    public FgExample get(int i, FeatureTemplateList fts);

    /** Gets the number of examples. */
    public int size();

}