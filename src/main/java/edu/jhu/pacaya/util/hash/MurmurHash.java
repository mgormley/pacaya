package edu.jhu.pacaya.util.hash;

import java.nio.ByteBuffer;

//package ie.ucd.murmur;

/** 
 * murmur hash 2.0.
 * 
 * The murmur hash is a relatively fast hash function from
 * http://murmurhash.googlepages.com/ for platforms with efficient
 * multiplication.
 * 
 * This is a re-implementation of the original C code plus some
 * additional features.
 * 
 * Public domain.
 * 
 * @author Viliam Holub
 * @version 1.0.2
 * 
 * FROM: https://github.com/tnm/murmurhash-java
 *
 */
public final class MurmurHash {
    
    private static final int DEFAULT_SEED = 0x9747b28c;

    // all methods static; private constructor. 
    private MurmurHash() {}

    public static int hash32(final long data) {
        return hash32(data, DEFAULT_SEED); 
    }

    /** My conversion of MurmurHash to take a long as input. */
    public static int hash32(final long data, int seed) {
     // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        final int length = 8;
        int h = seed^length;

        for (int i=0; i<2; i++) {
            int k = (i==0) ? (int) (data & 0xffffffffl) : (int) ((data >>> 32) & 0xffffffffl);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }
        
        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    public static int hash32(final long[] data, int length) {
        return hash32(data, length, DEFAULT_SEED);
    }
    
    public static int hash32(final long[] data, int length, int seed) {
        byte[] bytes = toByteArray(data, length);
        return hash32(bytes, bytes.length, seed);
    }

    public static byte[] toByteArray(final long[] data, int length) {
        final int byteLength = length * 8;
        ByteBuffer b = ByteBuffer.allocate(byteLength);
        for (int i=0; i<length; i++) {
            b.putLong(data[i]);
        }
        return b.array();
    }

    public static int hash32(final int[] data, int length) {
        return hash32(data, length, DEFAULT_SEED);
    }
    
    public static int hash32(final int[] data, int length, int seed) {
        byte[] bytes = toByteArray(data, length);
        return hash32(bytes, bytes.length, seed);
    }

    public static byte[] toByteArray(final int[] data, int length) {
        final int byteLength = length * 4;
        ByteBuffer b = ByteBuffer.allocate(byteLength);
        for (int i=0; i<length; i++) {
            b.putInt(data[i]);
        }
        return b.array();
    }

    public static int hash32(final short[] data, int length) {
        return hash32(data, length, DEFAULT_SEED);
    }
    
    public static int hash32(final short[] data, int length, int seed) {
        byte[] bytes = toByteArray(data, length);
        return hash32(bytes, bytes.length, seed);
    }

    public static byte[] toByteArray(final short[] data, int length) {
        final int byteLength = length * 2;
        ByteBuffer b = ByteBuffer.allocate(byteLength);
        for (int i=0; i<length; i++) {
            b.putShort(data[i]);
        }
        return b.array();
    }
    
    /** 
     * Generates 32 bit hash from byte array of the given length and
     * seed.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length, int seed) {
        // 'm' and 'r' are mixing constants generated offline.
        // They're not really 'magic', they just happen to work well.
        final int m = 0x5bd1e995;
        final int r = 24;

        // Initialize the hash to a random value
        int h = seed^length;
        int length4 = length/4;

        for (int i=0; i<length4; i++) {
            final int i4 = i*4;
            int k = (data[i4+0]&0xff) +((data[i4+1]&0xff)<<8)
                    +((data[i4+2]&0xff)<<16) +((data[i4+3]&0xff)<<24);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }
        
        // Handle the last few bytes of the input array
        switch (length%4) {
        case 3: h ^= (data[(length&~3) +2]&0xff) << 16;
        case 2: h ^= (data[(length&~3) +1]&0xff) << 8;
        case 1: h ^= (data[length&~3]&0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }
    
    /** 
     * Generates 32 bit hash from byte array with default seed value.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @return 32 bit hash of the given array
     */
    public static int hash32(final byte[] data, int length) {
        return hash32(data, length, 0x9747b28c); 
    }

    /** 
     * Generates 32 bit hash from a string.
     * 
     * @param text string to hash
     * @return 32 bit hash of the given string
     */
    public static int hash32(final String text) {
        final byte[] bytes = text.getBytes(); 
        return hash32(bytes, bytes.length);
    }

    /** 
     * Generates 32 bit hash from a substring.
     * 
     * @param text string to hash
     * @param from starting index
     * @param length length of the substring to hash
     * @return 32 bit hash of the given string
     */
    public static int hash32(final String text, int from, int length) {
        return hash32(text.substring( from, from+length));
    }
    
    /** 
     * Generates 64 bit hash from byte array of the given length and seed.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @param seed initial seed value
     * @return 64 bit hash of the given array
     */
    public static long hash64(final byte[] data, int length, int seed) {
        final long m = 0xc6a4a7935bd1e995L;
        final int r = 47;

        long h = (seed&0xffffffffl)^(length*m);

        int length8 = length/8;

        for (int i=0; i<length8; i++) {
            final int i8 = i*8;
            long k =  ((long)data[i8+0]&0xff)      +(((long)data[i8+1]&0xff)<<8)
                    +(((long)data[i8+2]&0xff)<<16) +(((long)data[i8+3]&0xff)<<24)
                    +(((long)data[i8+4]&0xff)<<32) +(((long)data[i8+5]&0xff)<<40)
                    +(((long)data[i8+6]&0xff)<<48) +(((long)data[i8+7]&0xff)<<56);
            
            k *= m;
            k ^= k >>> r;
            k *= m;
            
            h ^= k;
            h *= m; 
        }
        
        switch (length%8) {
        case 7: h ^= (long)(data[(length&~7)+6]&0xff) << 48;
        case 6: h ^= (long)(data[(length&~7)+5]&0xff) << 40;
        case 5: h ^= (long)(data[(length&~7)+4]&0xff) << 32;
        case 4: h ^= (long)(data[(length&~7)+3]&0xff) << 24;
        case 3: h ^= (long)(data[(length&~7)+2]&0xff) << 16;
        case 2: h ^= (long)(data[(length&~7)+1]&0xff) << 8;
        case 1: h ^= (long)(data[length&~7]&0xff);
                h *= m;
        };
     
        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        return h;
    }
    
    /** 
     * Generates 64 bit hash from byte array with default seed value.
     * 
     * @param data byte array to hash
     * @param length length of the array to hash
     * @return 64 bit hash of the given string
     */
    public static long hash64(final byte[] data, int length) {
        return hash64(data, length, 0xe17a1465);
    }

    /** 
     * Generates 64 bit hash from a string.
     * 
     * @param text string to hash
     * @return 64 bit hash of the given string
     */
    public static long hash64(final String text) {
        final byte[] bytes = text.getBytes(); 
        return hash64(bytes, bytes.length);
    }

    /** 
     * Generates 64 bit hash from a substring.
     * 
     * @param text string to hash
     * @param from starting index
     * @param length length of the substring to hash
     * @return 64 bit hash of the given array
     */
    public static long hash64(final String text, int from, int length) {
        return hash64(text.substring( from, from+length));
    }
}