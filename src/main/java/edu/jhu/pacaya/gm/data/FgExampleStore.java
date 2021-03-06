package edu.jhu.pacaya.gm.data;

/**
 * A mutable collection of instances for a graphical model represented as factor
 * graphs.
 * 
 * @author mgormley
 * 
 */
public interface FgExampleStore extends FgExampleList {

    /** Adds an example. */
    public void add(LFgExample example);

}
