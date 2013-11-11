package edu.jhu.util.cache;

import java.util.Iterator;
import java.util.Map;

class MapValueIterator<K,V> implements Iterator<V> {

    private Iterator<K> keyIter;
    private Map<K,V> map;
    
    public MapValueIterator(Iterator<K> keyIter, Map<K,V> map) {
        this.keyIter = keyIter;
        this.map = map;
    }
    
    @Override
    public boolean hasNext() {
        return keyIter.hasNext();
    }

    @Override
    public V next() {
        if (!hasNext()) {
            return null;
        }
        return map.get(keyIter.next());
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}