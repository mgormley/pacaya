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
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Disk-backed map with insert-only capabilities.
 * 
 * @author mgormley
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class FastDiskStore<K,V extends Serializable> {

    private static final int SIZE_OF_INT = Integer.SIZE / 8;
    // Map from key to position in the file.
    private HashMap<K,Long> keyPosMap;
    // File containing the values.
    private RandomAccessFile raf;
    // The current insertion position.
    private long curPos;
    // Whether to gzip serialized objects.
    private final boolean gzipOnSerialize;
        
    public FastDiskStore(File path, boolean gzipOnSerialize) throws FileNotFoundException {
        path.getParentFile().mkdirs();
        keyPosMap = new HashMap<K, Long>();
        raf = new RandomAccessFile(path, "rw");
        curPos = 0;
        this.gzipOnSerialize = gzipOnSerialize;
    }
    
    public void put(K key, V value) throws IOException {        
        raf.seek(curPos);
        byte[] valBytes = serialize(value);
        // Write the number of bytes.
        raf.writeInt(valBytes.length);
        // Write the bytes.
        raf.write(valBytes);
        // Add the position to the map.
        keyPosMap.put(key, curPos);
        // Increment the current position.
        curPos += SIZE_OF_INT;
        curPos += valBytes.length;
    }
    
    @SuppressWarnings("unchecked")
    public V get(K key) throws IOException {
        Long pos = keyPosMap.get(key);
        if (pos == null) {
            return null;
        }
        if (pos > raf.length()) {
            throw new IllegalStateException("Illegal position for key: " + key);
        }
        raf.seek(pos);
        int numBytes = raf.readInt();
        return (V) readBytes(numBytes);
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
    public byte[] serialize(Serializable obj) {
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
    public Object deserialize(byte[] bytes) {
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
    
}
