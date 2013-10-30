package edu.jhu.gm;

/**
 * A mutable collection of instances for a graphical model represented as factor
 * graphs.
 * 
 * @author mgormley
 * 
 */
public interface FgExamplesStore extends FgExamples {

    /** Adds an example. */
    public void add(FgExample example);

}
