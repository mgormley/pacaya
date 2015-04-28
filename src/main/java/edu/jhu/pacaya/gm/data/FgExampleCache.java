package edu.jhu.pacaya.gm.data;

import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.ReferenceMap;

import edu.jhu.pacaya.util.cache.GzipMap;

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
public class FgExampleCache extends AbstractFgExampleList implements FgExampleList {

    private FgExampleList exampleFactory;
    private Map<Integer, LFgExample> cache;

    /**
     * Constructor with a cache that uses SoftReferences.
     */
    public FgExampleCache(FgExampleList exampleFactory) {
        this(exampleFactory, -1, false);
    }

    /**
     * Constructor with LRU cache.
     * @param maxEntriesInMemory The maximum number of entries to keep in the
     *            in-memory cache or -1 to use a SoftReference cache.
     */
    @SuppressWarnings("unchecked")
    public FgExampleCache(FgExampleList exampleFactory, int maxEntriesInMemory, boolean gzipOnSerialize) {
        this.exampleFactory = exampleFactory;
        @SuppressWarnings("rawtypes")
        Map tmp;
        if (maxEntriesInMemory == -1) {
            tmp = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        } else {
            tmp = new LRUMap(maxEntriesInMemory);
        }
        if (gzipOnSerialize) {
            cache = new GzipMap<Integer, LFgExample>(tmp);
        } else {
            cache = tmp;
        }
    }

    /** Gets the i'th example. */
    public LFgExample get(int i) {
        LFgExample ex;
        synchronized (cache) {
            ex = cache.get(i);
        }
        if (ex == null) {            
            ex = exampleFactory.get(i);
            synchronized (cache) {
                cache.put(i, ex);
            }
        }
        return ex;
    }

    /** Gets the number of examples. */
    public synchronized int size() {
        return exampleFactory.size();
    }
    
    /** removes all cached FgExamples */
    public synchronized void clear() {
        cache.clear();
    }

}
