package edu.jhu.pacaya.util.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipMap<K, V extends Serializable> implements Map<K,V> {

    private Map<K,byte[]> map;
    
    /** Standard constructor which uses a HashMap internally. */
    public GzipMap() {
        this.map = new HashMap<K,byte[]>();
    }
    
    /** Wrapper constructor. */
    public GzipMap(Map<K,byte[]> map) {
        this.map = map;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        byte[] bytes = map.get(key);
        return (V) safeDeserialize(bytes);
    }

    private static Object safeDeserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return GzipMap.deserialize(bytes, true);
    }

    private static byte[] safeSerialize(Serializable value) {
        return GzipMap.serialize(value, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        byte[] bytes = map.put(key, safeSerialize(value));
        return (V) safeDeserialize(bytes);
    }
    
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value instanceof Serializable) {
            return map.containsValue(safeSerialize((Serializable) value));
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        return (V) safeDeserialize(map.remove(key));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K,? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    public Iterator<V> valueIterator() {
        return new MapValueIterator<K,V>(keySet().iterator(), this);
    }
    
    @Override
    public Collection<V> values() {
        return new IteratorOnlyCollection<V>(valueIterator());
    }

    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        // TODO: Implement this.
        throw new UnsupportedOperationException();
    }

    /** Deserialize and ungzip an object. */
    public static Object deserialize(byte[] bytes, boolean gzipOnSerialize) {
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            if (gzipOnSerialize) {
                is = new GZIPInputStream(is);
            }
            ObjectInputStream in = new ObjectInputStream(is);
            Object inObj = in.readObject();
            in.close();
            return inObj;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /** Serializes and gzips an object. */
    public static byte[] serialize(Serializable obj, boolean gzipOnSerialize) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out;
            if (gzipOnSerialize) { 
                out = new ObjectOutputStream(new GZIPOutputStream(baos));
            } else {
                out = new ObjectOutputStream(baos);
            }
            out.writeObject(obj);
            out.flush();
            out.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
