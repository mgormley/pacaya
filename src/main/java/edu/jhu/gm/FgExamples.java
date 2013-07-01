package edu.jhu.gm;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A collection of samples for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public class FgExamples implements Iterable<FgExample> {
        
    private ArrayList<FgExample> examples;

    public FgExamples() {
        examples = new ArrayList<FgExample>();
    }
    
    /** Adds an example. */
    public void add(FgExample example) {
        examples.add(example);
    }
    
    /** Gets the i'th example. */
    public FgExample get(int i) {
        return examples.get(i);
    }
    
    /** Gets the number of examples. */
    public int size() {
        return examples.size();
    }

    @Override
    public Iterator<FgExample> iterator() {
        return examples.iterator();
    }
    
}
