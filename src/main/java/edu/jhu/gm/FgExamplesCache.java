package edu.jhu.gm;

import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * An immutable collection of instances for a graphical model.
 * 
 * This implementation assumes that the given examplesFactory requires some slow
 * computation for each call to get(i). Accordingly, a cache is placed in front
 * of the factory to reduce the number of calls.
 * 
 * @author mgormley
 * 
 */
public class FgExamplesCache extends AbstractFgExamples implements FgExamples {

    private FgExamples exampleFactory;
    private Map<Integer, FgExample> cache;

    /**
     * Constructor with a cache that uses SoftReferences.
     */
    public FgExamplesCache(FeatureTemplateList fts, FgExamples exampleFactory) {
        this(fts, exampleFactory, -1);
    }

    /**
     * Constructor with LRU cache.
     * 
     * @param maxEntriesInMemory The maximum number of entries to keep in the
     *            in-memory cache or -1 to use a SoftReference cache.
     */
    @SuppressWarnings("unchecked")
    public FgExamplesCache(FeatureTemplateList fts, FgExamples exampleFactory, int maxEntriesInMemory) {
        super(fts);
        this.exampleFactory = exampleFactory;
        if (maxEntriesInMemory == -1) {
            cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        } else {
            cache = new LRUMap(maxEntriesInMemory);
        }
    }

    /** Gets the i'th example. */
    public FgExample get(int i) {
        FgExample ex = cache.get(i);
        if (ex == null) {            
            ex = exampleFactory.get(i);
            cache.put(i, ex);
        }
        return ex;
    }

    /** Gets the number of examples. */
    public int size() {
        return exampleFactory.size();
    }

}
