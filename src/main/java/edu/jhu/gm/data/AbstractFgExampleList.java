package edu.jhu.gm.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An abstract collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public abstract class AbstractFgExampleList implements FgExampleList {
        
    public AbstractFgExampleList() { }

    /** Gets the i'th example. */
    public abstract LFgExample get(int i);
    
    /** Gets the number of examples. */
    public abstract int size();
    
    // -------- Methods which require iteration through the entire collection ------------
    //
    // TODO: For the disk-backed and caching versions of FgExamples these will be very slow.
    
    public int getNumFactors() {
        int numFactors = 0;
        for (LFgExample ex : this) {
            numFactors += ex.getOriginalFactorGraph().getNumFactors();
        }
        return numFactors;
    }

    public int getNumVars() {
        int numVars = 0;
        for (LFgExample ex : this) {
            numVars += ex.getOriginalFactorGraph().getNumVars();
        }
        return numVars;
    }
    
    // ------------------------------------------------------------------------------

    public Iterator<LFgExample> iterator() {
        return new Itr();
    }
    
    private class Itr implements Iterator<LFgExample> {
        
        private int cur = 0;

        @Override
        public boolean hasNext() {
            return cur < size();
        }

        @Override
        public LFgExample next() {
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
