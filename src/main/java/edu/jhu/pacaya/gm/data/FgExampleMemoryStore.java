package edu.jhu.pacaya.gm.data;

import java.util.ArrayList;

/**
 * A simple in-memory mutable collection of instances for a graphical model
 * represented as factor graphs.
 * 
 * @author mgormley
 * 
 */
public class FgExampleMemoryStore implements FgExampleStore {

    // Note: ArrayList is not synchronized, so we must synchronize access to
    // this class.
    private ArrayList<LFgExample> examples;

    public FgExampleMemoryStore() {
        this.examples = new ArrayList<LFgExample>();
    }

    /** Adds an example. */
    public synchronized void add(LFgExample example) {
        examples.add(example);
    }

    /** Gets the i'th example. */
    public synchronized LFgExample get(int i) {
        return examples.get(i);
    }

    /** Gets the number of examples. */
    public synchronized int size() {
        return examples.size();
    }

}
