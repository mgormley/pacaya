package edu.jhu.pacaya.util.cache;

import java.util.AbstractSequentialList;
import java.util.Iterator;
import java.util.ListIterator;

/** A collection which only supports iteration. */    
public class IteratorOnlyCollection<T> extends AbstractSequentialList<T> {

    private Iterator<T> iterator;
            
    public IteratorOnlyCollection(Iterator<T> iterator) {
        this.iterator = iterator;
    }
    
    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }
    
}