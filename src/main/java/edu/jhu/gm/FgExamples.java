package edu.jhu.gm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.util.Utilities;
import edu.jhu.util.cache.CachedFastDiskStore;

/**
 * A collection of samples for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public class FgExamples {
        
    private CachedFastDiskStore<Integer, FgExample> examples;
    
    /**
     * This is a hack to carry around the source sentences. For example, the
     * CoNLL2009 sentences from which these examples were generated.
     */
    private Object sourceSents;
    private FeatureTemplateList fts;

    public FgExamples(FeatureTemplateList fts) {
        this.fts = fts;
        try {
            File cachePath = File.createTempFile("cache", ".binary.gz", new File("."));
            this.examples = new CachedFastDiskStore<Integer, FgExample>(cachePath, true);
            cachePath.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Adds an example. */
    public void add(FgExample example) {
        try {
            examples.put(examples.size(), example);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Gets the i'th example. */
    public FgExample get(int i) {
        try {
            return examples.get(i);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Gets the number of examples. */
    public int size() {
        return examples.size();
    }

    public List<VarConfig> getGoldConfigs() {
        List<VarConfig> varConfigList = new ArrayList<VarConfig>();
        for (FgExample ex : Utilities.asIterable(examples.valueIterator())) {
            varConfigList.add(ex.getGoldConfig());
        }
        return varConfigList;
    }

    public int getNumFactors() {
        int numFactors = 0;
        for (FgExample ex : Utilities.asIterable(examples.valueIterator())) {
            numFactors += ex.getOriginalFactorGraph().getNumFactors();
        }
        return numFactors;
    }

    public int getNumVars() {
        int numVars = 0;
        for (FgExample ex : Utilities.asIterable(examples.valueIterator())) {
            numVars += ex.getOriginalFactorGraph().getNumVars();
        }
        return numVars;
    }

    public void setSourceSentences(Object sents) {
        this.sourceSents = sents;
    }
    
    public Object getSourceSentences() {
        return sourceSents;
    }

    public FeatureTemplateList getTemplates() {
        return fts;
    }

    public double getTotMsFgClampTimer() {
        double tot = 0;
        for (int i=0; i<this.size(); i++) {
            tot += this.get(i).fgClampTimer.totMs();
        }
        return tot;
    }

    public double getTotMsFeatCacheTimer() {
        double tot = 0;
        for (int i=0; i<this.size(); i++) {
            tot += this.get(i).featCacheTimer.totMs();
        }
        return tot;
    }

}
