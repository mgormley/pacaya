package edu.jhu.gm.data;

import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.gm.feat.FactorTemplateList;

/**
 * A simple in-memory mutable collection of instances for a graphical model
 * represented as factor graphs.
 * 
 * @author mgormley
 * 
 */
public class FgExampleMemoryStore extends AbstractFgExampleList implements FgExampleStore {

    // Note: ArrayList is not synchronized, so we must synchronize access to
    // this class.
    private ArrayList<FgExample> examples;

    public FgExampleMemoryStore(FactorTemplateList fts) {
        super(fts);
        this.examples = new ArrayList<FgExample>();
    }

    /** Adds an example. */
    public synchronized void add(FgExample example) {
        examples.add(example);
    }

    /** Gets the i'th example. */
    public synchronized FgExample get(int i) {
        return examples.get(i);
    }

    /** Gets the number of examples. */
    public synchronized int size() {
        return examples.size();
    }

}
