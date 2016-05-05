package edu.jhu.pacaya.sch.util;

import java.util.HashMap;
import java.util.function.Function;

public class DefaultDict<K, V> extends HashMap<K, V> {

    private static final long serialVersionUID = 1L;

    /**
     * a function to create a new value V v from the key K k 
     */
    Function<K, V> makeDefault = null;
    
    public DefaultDict(Function<K, V> makeDefault) {
        this.makeDefault = makeDefault;
    }

    /**
     * Adds the key using makeDefault to construct the value
     */
    public void add(K key) {
        // ignore the return value
        get(key);
    }
    
    /**
     * If the key isn't in the dictionary, the makeDefault function will be called to create a new value
     * which will be added
     */
    @Override
    public V get(Object key) {
        if (super.containsKey(key)) {
            return super.get(key);    
        } else {
            @SuppressWarnings("unchecked")
            K k = (K) key;
            V v = makeDefault.apply(k);
            put(k, v);
            return v;
        }
    }
    
}
