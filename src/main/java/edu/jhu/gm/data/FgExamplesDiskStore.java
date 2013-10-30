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
public class FgExamplesDiskStore extends AbstractFgExamples implements FgExamplesStore {

    private CachedFastDiskStore<Integer, FgExample> examples;

    public FgExamplesDiskStore(FeatureTemplateList fts) {
        this(fts, new File("."), true, -1);
    }

    public FgExamplesDiskStore(FeatureTemplateList fts, File cacheDir, boolean gzipped, int maxEntriesInMemory) {
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
    public void add(FgExample example) {
        examples.put(examples.size(), example);
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
        return examples.valueIterator();
    }

}
