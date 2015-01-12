package edu.jhu.gm.data;



/**
 * A collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public interface FgExampleList extends Iterable<LFgExample> {

    /** Gets the i'th example. */
    public LFgExample get(int i);
    
    /** Gets the number of examples. */
    public int size();
    
}
