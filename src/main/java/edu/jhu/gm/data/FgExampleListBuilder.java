package edu.jhu.gm.data;

import java.io.File;

import org.apache.log4j.Logger;

import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;
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

    /**
     * Combines an FgExampleFactory and a FeatureTemplateList into an FgExamples
     * object.
     * 
     * @author mgormley
     */
    public static class FgExampleFactoryWrapper extends AbstractFgExampleList implements FgExampleList {

        private FgExampleFactory factory;

        public FgExampleFactoryWrapper(FactorTemplateList fts, FgExampleFactory factory) {
            super(fts);
            this.factory = factory;
        }

        @Override
        public FgExample get(int i) {
            return factory.get(i, fts);
        }

        @Override
        public int size() {
            return factory.size();
        }

    }

    public enum CacheType {
        MEMORY_STORE, CACHE, DISK_STORE, NONE
    }

    /** Parameters for FgExamplesBuilder. */
    public static class FgExamplesBuilderPrm {

        /**
         * Minimum number of times (inclusive) a feature must occur in training
         * to be included in the model. Ignored if non-positive. (Using this
         * cutoff implies that unsupported features will not be included.)
         */
        public int featCountCutoff = -1;

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

    private static final Logger log = Logger.getLogger(FgExampleListBuilder.class);

    private FgExamplesBuilderPrm prm;

    public FgExampleListBuilder(FgExamplesBuilderPrm prm) {
        this.prm = prm;
    }

    public FgExampleList getInstance(FactorTemplateList fts, FgExampleFactory factory) {
        boolean doingFeatCutoff = (fts.isGrowing() && prm.featCountCutoff > 0);

        if (doingFeatCutoff) {
            log.info("Applying feature count cutoff: " + prm.featCountCutoff);
            cutoffFeatures(prm.featCountCutoff, fts, factory);
        }

        // Populate the feature template list and stop growth on it.
        FgExampleList data = new FgExampleFactoryWrapper(fts, factory);
        if (prm.cacheType == CacheType.CACHE) {
            data = new FgExampleCache(fts, data, prm.maxEntriesInMemory, prm.gzipped);
            constructAndDiscardAll(data);
        } else if (prm.cacheType == CacheType.MEMORY_STORE) {
            FgExampleStore store = new FgExampleMemoryStore(fts);
            constructAndStoreAll(data, store);
            data = store;
        } else if (prm.cacheType == CacheType.DISK_STORE) {
            FgExampleStore store = new FgExampleDiskStore(fts, prm.cacheDir, prm.gzipped, prm.maxEntriesInMemory);
            constructAndStoreAll(data, store);
            data = store;
        } else if (prm.cacheType == CacheType.NONE) {
            // Do nothing.
            if (!doingFeatCutoff) {
                // We still have to populate the FactorTemplateList before stopping growth on it.
                constructAndDiscardAll(data);
            }
        } else {
            throw new IllegalStateException("Unsupported cache type: " + prm.cacheType);
        }

        fts.stopGrowth();

        return data;
    }

    /**
     * Populate and stop growth on the fts by creating each example and keeping
     * only those features which pass the feature count cutoff threshold, and
     * bias features.
     * 
     * @param featCountCutoff Minimum number of times (inclusive) a feature must
     *            occur in training to be included in the model. Ignored if
     *            non-positive. (Using this cutoff implies that unsupported
     *            features will not be included.)
     * @param fts The output feature templates to which the count-cutoff
     *            features should be added. Growth on these feature templates
     *            will be stopped at the end of this function call.
     * @param factory The factory generating the examples.
     */
    public static void cutoffFeatures(int featCountCutoff, FactorTemplateList fts, FgExampleFactory factory) {
        // Use counting alphabets in this ftl.
        FactorTemplateList countFts = new FactorTemplateList(true);
        constructAndDiscardAll(countFts, factory);

        log.info("Num features before cutoff: " + countFts.getNumObsFeats());
        for (int t = 0; t < countFts.size(); t++) {
            FactorTemplate template = countFts.get(t);
            CountingAlphabet<Feature> countAlphabet = (CountingAlphabet<Feature>) template.getAlphabet();

            // Create a copy of this template, with a new alphabet.
            Alphabet<Feature> alphabet = new Alphabet<Feature>();
            fts.add(new FactorTemplate(template.getVars(), alphabet, template.getKey()));

            // Discard the features which occurred fewer times than the cutoff.
            for (int i = 0; i < countAlphabet.size(); i++) {
                int count = countAlphabet.lookupObjectCount(i);
                Feature feat = countAlphabet.lookupObject(i);
                // Always keep bias features.
                if (count >= featCountCutoff || feat.isBiasFeature()) {
                    alphabet.lookupIndex(feat);
                }
            }
            alphabet.stopGrowth();
        }
    }

    public static void constructAndDiscardAll(FactorTemplateList fts, FgExampleFactory factory) {
        constructAndDiscardAll(new FgExampleFactoryWrapper(fts, factory));
    }

    public static void constructAndDiscardAll(FgExampleList examples) {
        constructAndStoreAll(examples, null);
    }

    public static void constructAndStoreAll(FgExampleList examples, FgExampleStore store) {
        Timer fgTimer = new Timer();
        double totFgClampMs = 0;
        double totFeatCacheMs = 0;
        for (int i = 0; i < examples.size(); i++) {
            if (i % 1000 == 0 && i > 0) {
                log.debug("Preprocessed " + i + " examples...");
            }
            // Construct the example to update counter, and then discard it.
            fgTimer.start();
            FgExample ex = examples.get(i);
            if (store != null) {
                store.add(ex);
            }
            fgTimer.stop();

            // Get time spent on certain subtasks.
            totFgClampMs += ex.fgClampTimer.totMs();
            totFeatCacheMs += ex.featCacheTimer.totMs();
        }

        log.info("Time (ms) to clamp factor graphs: " + totFgClampMs);
        log.info("Time (ms) to cache features: " + totFeatCacheMs);
        log.info("Time (ms) to construct factor graph: " + (fgTimer.totMs() - totFgClampMs - totFeatCacheMs));
    }
}
