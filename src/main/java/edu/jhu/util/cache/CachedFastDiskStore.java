package edu.jhu.util.cache;

import java.io.File;
import java.io.FileNotFoundException;
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
public class CachedFastDiskStore<K,V extends Serializable> extends FastDiskStore<K, V> {

    private Map<K,V> cache;

    /**
     * Constructor with a cache that uses SoftReferences.
     * 
     * @param path The file to use as the disk store.
     * @param gzipOnSerialize Whether to gzip the objects after serializing
     *            them, before writing them to disk.
     * @throws FileNotFoundException
     */
    public CachedFastDiskStore(File path, boolean gzipOnSerialize) throws FileNotFoundException {
        this(path, gzipOnSerialize, -1);
    }

    /**
     * Constructor with LRU cache.
     * 
     * @param path The file to use as the disk store.
     * @param gzipOnSerialize Whether to gzip the objects after serializing
     *            them, before writing them to disk.
     * @param maxEntriesInMemory The maximum number of entries to keep in the
     *            in-memory cache or -1 to use a SoftReference cache.
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    public CachedFastDiskStore(File path, boolean gzipOnSerialize, int maxEntriesInMemory) throws FileNotFoundException {    
        super(path, gzipOnSerialize);
        if (maxEntriesInMemory == -1) {
            cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        } else {
            cache = new LRUMap(maxEntriesInMemory);
        }
    }

    public V put(K key, V value) {  
        V oldValue = super.put(key, value);
        cache.put(key, value);
        return oldValue;
    }
    
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = cache.get(key);
        if (value == null) {
            // Get the value from disk and put it in the cache.
            value = super.get(key);
            if (value != null) {
                cache.put((K) key, value);
            }
        }
        return value;
    }
    
}
