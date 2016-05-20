package edu.jhu.pacaya.gm.feat;

import java.io.Serializable;
import java.util.Iterator;

public class StringIterable implements Iterable<String>, Iterator<String>, Serializable{

    private static final long serialVersionUID = 1L;
    private Iterable<?> iterable;
    private Iterator<?> iter;
    public StringIterable(Iterable<?> iterable) {
        this.iterable = iterable;
    }
    
    @Override
    public Iterator<String> iterator() { 
        iter = iterable.iterator();
        return this;
    }

    @Override
    public boolean hasNext() {
        return iter.hasNext();
    }

    @Override
    public String next() {
        Object next = iter.next();
        if (next == null) {
            return "null";
        }   
        return next.toString();
    }

    @Override
    public void remove() {
        iter.remove();
    }
    
}