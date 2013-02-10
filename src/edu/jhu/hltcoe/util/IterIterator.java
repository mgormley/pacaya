package edu.jhu.hltcoe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Concatenates multiple iterators.
 * 
 * @author mgormley
 *
 */
public class IterIterator<X> implements Iterator<X> {

    private List<Iterator<X>> iters;
    private int i; 
    
    public IterIterator(Iterator<X> iter1, Iterator<X> iter2) {
        this.iters = new ArrayList<Iterator<X>>(2);
        iters.add(iter1);
        iters.add(iter2);
    }
    
    // This constructor causes warnings since the varargs don't preserve the generics for some reason.
    public IterIterator(Iterator<X>... iters) {
        this.iters = new ArrayList<Iterator<X>>(Arrays.asList(iters));
        i = 0;
    }
    
    @Override
    public boolean hasNext() {
        while(true) {
            if (i >= iters.size()) {
                return false;
            } else if (iters.get(i).hasNext()) {
                return true;
            } else {
                i++;
            }
        }
    }

    @Override
    public X next() {
        if (hasNext()) {
            return iters.get(i).next();
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new RuntimeException("not implemented");
    }

    
    
}
