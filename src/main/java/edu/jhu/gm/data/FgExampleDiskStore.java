package edu.jhu.gm.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import edu.jhu.gm.feat.FeatureTemplateList;
import edu.jhu.util.cache.CachedFastDiskStore;

/**
 * A disk-backed mutable collection of instances for a graphical model.
 * 
 * @author mgormley
 * 
 */
public class FgExampleDiskStore extends AbstractFgExampleList implements FgExampleStore {

    private CachedFastDiskStore<Integer, FgExample> examples;

    public FgExampleDiskStore(FeatureTemplateList fts) {
        this(fts, new File("."), true, -1);
    }

    public FgExampleDiskStore(FeatureTemplateList fts, File cacheDir, boolean gzipped, int maxEntriesInMemory) {
        super(fts);
        try {
            File cachePath = File.createTempFile("cache", ".binary.gz", cacheDir);
            this.examples = new CachedFastDiskStore<Integer, FgExample>(cachePath, gzipped, maxEntriesInMemory);
            // TODO: cachePath.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Adds an example. */
    public synchronized void add(FgExample example) {
        examples.put(examples.size(), example);
    }

    /** Gets the i'th example. */
    public synchronized FgExample get(int i) {
        return examples.get(i);
    }

    /** Gets the number of examples. */
    public synchronized int size() {
        return examples.size();
    }

    // In an old version of this class, we used the following iterator. 
    // However there was no way to ensure its thread safety. The iterator
    // in the abstract base class however, relies on get(i) which is thread 
    // safe.
    //
    //    public Iterator<FgExample> iterator() {
    //        return examples.valueIterator();
    //    }

}
