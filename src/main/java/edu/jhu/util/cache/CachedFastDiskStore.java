package edu.jhu.util.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.ReferenceMap;

/**
 * Fast disk store with an in-memory cache in front of it.
 * 
 * @author mgormley
 * @param <K> The key type.
 * @param <V> The value type.
 */
// TODO: make the cache itself only a SoftReference.
public class CachedFastDiskStore<K,V extends Serializable> {

    private FastDiskStore<K, V> fds;
    private Map<K,V> cache;
    
    /**
     * Constructor with a cache that uses SoftReferences.
     * @param path
     * @param gzipOnSerialize
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    public CachedFastDiskStore(File path, boolean gzipOnSerialize) throws FileNotFoundException {    
        fds = new FastDiskStore<K,V>(path, gzipOnSerialize);
        cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }
    
    /**
     * Constructor with LRU cache.
     * @param path
     * @param gzipOnSerialize
     * @param maxEntriesInMemory
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    public CachedFastDiskStore(File path, boolean gzipOnSerialize, int maxEntriesInMemory) throws FileNotFoundException {    
        fds = new FastDiskStore<K,V>(path, gzipOnSerialize);
        cache = new LRUMap(maxEntriesInMemory);
    }

    public void put(K key, V value) throws IOException {  
        fds.put(key, value);
        cache.put(key, value);
    }
    
    public V get(K key) throws IOException {
        V value = cache.get(key);
        if (value == null) {
            // Get the value from disk and put it in the cache.
            value = fds.get(key);
            cache.put(key, value);
        }
        return value;
    }
    
}
