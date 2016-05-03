package edu.jhu.pacaya.gm.data;

import java.util.Iterator;
import java.util.NoSuchElementException;



/**
 * A collection of instances for a graphical model represented as factor graphs.
 * 
 * @author mgormley
 *
 */
public interface FgExampleList  {

    /** Gets the i'th example. */
    public LFgExample get(int i);
    
    /** Gets the number of examples. */
    public int size();

    public default Iterator<LFgExample> iterator() {
        return new Iterator<LFgExample>() {
            
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
            
        };
    }
    
}
