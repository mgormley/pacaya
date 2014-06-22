package edu.jhu.util.hash;

import static org.junit.Assert.*;

import org.junit.Test;

public class MurmurHash3Test {

    @Test
    public void testConcat() {
        byte[] data = new byte[]{0,1,2,3,4,5,6,7};
        int seed = 123456789;
        int hash1 = MurmurHash3.murmurhash3_x86_32(data, 0, 4, seed);
        int hash2 = MurmurHash3.murmurhash3_x86_32(data, 4, 4, seed);
        int hash3 = MurmurHash3.murmurhash3_x86_32(data, 0, 8, seed);
        System.out.println("hash1: " + hash1);
        System.out.println("hash2: " + hash2);
        System.out.println("hash3: " + hash3);
        System.out.println("hash1 * hash2: " + hash1 * hash2);
    }

}
