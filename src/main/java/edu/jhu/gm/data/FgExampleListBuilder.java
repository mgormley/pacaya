package edu.jhu.gm.data;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.util.Prm;
import edu.jhu.util.Timer;

/**
 * Factory for FgExamples.
 * 
 * This neatly packages up several important features: (1) feature count cutoffs
 * and (2) caching of examples.
 * 
 * These two are intertwined in that both features affect the growth of the
 * feature template alphabets.
 * 
 * @author mgormley
 * @author mmitchell
 */
public class FgExampleListBuilder {

    public enum CacheType {
        MEMORY_STORE, CACHE, DISK_STORE, NONE
    }

    /** Parameters for FgExamplesBuilder. */
    public static class FgExamplesBuilderPrm extends Prm {
        
        private static final long serialVersionUID = 1L;

        /** The type of FgExamples object to wrap the factory in. */
        public CacheType cacheType = CacheType.MEMORY_STORE;

        /**
         * The maximum number of entries to keep in a memory-cache, or -1 to use
         * a SoftReference cache.
         */
        public int maxEntriesInMemory = -1;

        /** Whether to GZip the disk cache. */
        public boolean gzipped = true;

        /** The directory in which the disk store file should be created. */
        public File cacheDir = new File(".");
    }

    private static final Logger log = LoggerFactory.getLogger(FgExampleListBuilder.class);

    private FgExamplesBuilderPrm prm;

    public FgExampleListBuilder(FgExamplesBuilderPrm prm) {
        this.prm = prm;
    }

    public FgExampleList getInstance(FgExampleList data) {
        if (prm.cacheType == CacheType.CACHE) {
            data = new FgExampleCache(data, prm.maxEntriesInMemory, prm.gzipped);
        } else if (prm.cacheType == CacheType.MEMORY_STORE) {
            FgExampleStore store = new FgExampleMemoryStore();
            constructAndStoreAll(data, store);
            data = store;
        } else if (prm.cacheType == CacheType.DISK_STORE) {
            FgExampleStore store = new FgExampleDiskStore(prm.cacheDir, prm.gzipped, prm.maxEntriesInMemory);
            constructAndStoreAll(data, store);
            data = store;
        } else if (prm.cacheType == CacheType.NONE) {
            // Do nothing.
        } else {
            throw new IllegalStateException("Unsupported cache type: " + prm.cacheType);
        }

        return data;
    }

    public static void constructAndStoreAll(FgExampleList examples, FgExampleStore store) {
        Timer fgTimer = new Timer();
        double totFgClampMs = 0;
        for (int i = 0; i < examples.size(); i++) {
            if (i % 1000 == 0 && i > 0) {
                log.debug("Preprocessed " + i + " examples...");
            }
            // Construct the example to update counter, and then discard it.
            fgTimer.start();
            LFgExample ex = examples.get(i);
            if (store != null) {
                store.add(ex);
            }
            fgTimer.stop();

            // Get time spent on certain subtasks.
            totFgClampMs += ((UnlabeledFgExample)ex).fgClampTimer.totMs();
        }

        log.info("Time (ms) to clamp factor graphs: " + totFgClampMs);
        log.info("Time (ms) to construct factor graph: " + (fgTimer.totMs() - totFgClampMs));
    }
    
}
