package edu.jhu.gm.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import edu.jhu.gm.feat.FeatureTemplateList;

/**
 * An abstract collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public abstract class AbstractFgExampleList implements FgExampleList {
        
    /**
     * This is a hack to carry around the source sentences. For example, the
     * CoNLL2009 sentences from which these examples were generated.
     */
    private Object sourceSents;
    protected FeatureTemplateList fts;

    public AbstractFgExampleList(FeatureTemplateList fts) {
        this.fts = fts;
    }

    /** Gets the i'th example. */
    public abstract FgExample get(int i);
    
    /** Gets the number of examples. */
    public abstract int size();

    public void setSourceSentences(Object sents) {
        this.sourceSents = sents;
    }
    
    public Object getSourceSentences() {
        return sourceSents;
    }

    public FeatureTemplateList getTemplates() {
        return fts;
    }
    
    // -------- Methods which require iteration through the entire collection ------------
    //
    // TODO: For the disk-backed and caching versions of FgExamples these will be very slow.
    
    public int getNumFactors() {
        int numFactors = 0;
        for (FgExample ex : this) {
            numFactors += ex.getOriginalFactorGraph().getNumFactors();
        }
        return numFactors;
    }

    public int getNumVars() {
        int numVars = 0;
        for (FgExample ex : this) {
            numVars += ex.getOriginalFactorGraph().getNumVars();
        }
        return numVars;
    }

    public double getTotMsFgClampTimer() {
        double tot = 0;
        for (FgExample ex : this) {
            tot += ex.fgClampTimer.totMs();
        }
        return tot;
    }

    public double getTotMsFeatCacheTimer() {
        double tot = 0;
        for (FgExample ex : this) {
            tot += ex.featCacheTimer.totMs();
        }
        return tot;
    }
    
    // ------------------------------------------------------------------------------

    public Iterator<FgExample> iterator() {
        return new Itr();
    }
    
    private class Itr implements Iterator<FgExample> {
        
        private int cur = 0;

        @Override
        public boolean hasNext() {
            return cur < size();
        }

        @Override
        public FgExample next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return get(cur++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
}
