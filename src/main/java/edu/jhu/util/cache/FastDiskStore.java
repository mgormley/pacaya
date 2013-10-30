package edu.jhu.util.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Disk-backed map with insert-only capabilities.
 * 
 * @author mgormley
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FastDiskStore<K,V extends Serializable> implements Map<K, V> {

    private static final int SIZE_OF_INT = Integer.SIZE / 8;
    // Map from key to position in the file.
    private LinkedHashMap<K,Long> keyPosMap;
    // File containing the values.
    private RandomAccessFile raf;
    // The current insertion position.
    private long curPos;
    // Whether to gzip serialized objects.
    private final boolean gzipOnSerialize;
    // The number of entries.
    private int numEntries;
        
    public FastDiskStore(File path, boolean gzipOnSerialize) throws FileNotFoundException {
        path.getParentFile().mkdirs();
        keyPosMap = new LinkedHashMap<K, Long>();
        raf = new RandomAccessFile(path, "rw");
        curPos = 0;
        numEntries = 0;
        this.gzipOnSerialize = gzipOnSerialize;
    }
    
    @Override
    public V put(K key, V value) {   
        if (keyPosMap.containsKey(key)) {
            // TODO: support multiple puts per key.
            throw new IllegalStateException("FastDiskStore currently only supports one put call per key.");
        }
        byte[] valBytes = serialize(value);
        try {
            raf.seek(curPos);
            // Write the number of bytes.
            raf.writeInt(valBytes.length);
            // Write the bytes.
            raf.write(valBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Add the position to the map.
        keyPosMap.put(key, curPos);
        // Increment the current position.
        curPos += SIZE_OF_INT;
        curPos += valBytes.length;        
        numEntries++;
        
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        Long pos = keyPosMap.get(key);
        if (pos == null) {
            return null;
        }
        try {
            if (pos > raf.length()) {
                throw new IllegalStateException("Illegal position for key: " + key);
            }
            raf.seek(pos);
            int numBytes = raf.readInt();
            return (V) readBytes(numBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int size() {
        return numEntries;
    }
    
    private Object readBytes(int numBytes) throws IOException {
        byte[] bytes = new byte[numBytes];
        int numRead = raf.read(bytes);
        if (numRead != numBytes) {
            throw new IllegalStateException("Invalid number of bytes read: " + numRead);
        }
        return deserialize(bytes);
    }
    

    /** Serializes and gzips an object. */
    private byte[] serialize(Serializable obj) {
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

    /** Deserialize and ungzip an object. */
    private Object deserialize(byte[] bytes) {
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
    
    public Iterator<K> keyIterator() {
        return keyPosMap.keySet().iterator();
    }
    
    public Iterator<V> valueIterator() {
        return new FastDiskStoreValueIterator();
    }  
    
    private class FastDiskStoreValueIterator implements Iterator<V> {

        private Iterator<K> keyIter = keyIterator();
        
        @Override
        public boolean hasNext() {
            return keyIter.hasNext();
        }

        @Override
        public V next() {
            if (!hasNext()) {
                return null;
            }
            return get(keyIter.next());
        }

        @Override
        public void remove() {
            throw new RuntimeException("not implemented");
        }
        
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Set<K> keySet() {
        return keyPosMap.keySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return keyPosMap.containsKey(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Collection<V> values() {      
        return new IteratorOnlyCollection<V>(valueIterator());
    }
    
    /** A collection which only supports iteration. */    
    private static class IteratorOnlyCollection<T> extends AbstractSequentialList<T> {

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
    
    // ----------------------- Unsupported Operations --------------------

    /** @throws UnsupportedOperationException */
    @Override
    public void clear() {
        // TODO: Support removal.
        // numEntries = 0;
        // curPos = 0;
        // keyPosMap.clear();
        throw new UnsupportedOperationException();
    }
    
    /** @throws UnsupportedOperationException */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /** @throws UnsupportedOperationException */
    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /** @throws UnsupportedOperationException */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }
    
}
