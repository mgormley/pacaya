package edu.jhu.gm.data;

import java.util.ArrayList;
import java.util.Iterator;

import edu.jhu.gm.feat.FeatureTemplateList;

/**
 * A simple in-memory mutable collection of instances for a graphical model
 * represented as factor graphs.
 * 
 * @author mgormley
 * 
 */
public class FgExamplesMemoryStore extends AbstractFgExamples implements FgExamplesStore {

    private ArrayList<FgExample> examples;

    public FgExamplesMemoryStore(FeatureTemplateList fts) {
        super(fts);
        this.examples = new ArrayList<FgExample>();
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

    public Iterator<FgExample> iterator() {
        return examples.iterator();
    }

}
