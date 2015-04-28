package edu.jhu.pacaya.util.hash;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;


public class MurmurHashTest {

    @Test
    public void testMurmurHashLong() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.put((byte) 1);
        bb.put((byte) 2);
        bb.put((byte) 3);
        bb.put((byte) 4);
        bb.put((byte) 5);
        bb.put((byte) 6);
        bb.put((byte) 7);
        bb.put((byte) 8);
        long l1 = bb.getLong(0);
        long l2 = (1l << 0) | (2l << 8) | (3l << 16) | (4l << 24) | (5l << 32)
                | (6l << 40) | (7l << 48) | (8l << 56);
        assertTrue(l1 != l2);
        
        int hash1 = MurmurHash.hash32(bb.array(), 8);
        int hash2 = MurmurHash.hash32(l1);
        int hash3 = MurmurHash.hash32(l2);
        assertEquals(hash1, hash3);
        assertTrue(hash1 != hash2);
    }

}
