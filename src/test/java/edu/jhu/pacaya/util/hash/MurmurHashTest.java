package edu.jhu.pacaya.util.hash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Assert;
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

    @Test
    public void testToByteArrayShortArrayInt() throws Exception {
        byte[] bs = new byte[4];
        bs[0] = 1;
        bs[1] = 2;
        bs[2] = 3;
        bs[3] = 4;
        short[] ss = new short[4];
        ss[0] = (2 << 0) | (1 << 8);
        ss[1] = (4 << 0) | (3 << 8);

        byte[] bsNew = MurmurHash.toByteArray(ss, 2);
        System.out.println("expected: " + Arrays.toString(bs));
        System.out.println("actual  : " + Arrays.toString(bsNew));
        Assert.assertArrayEquals(bs, bsNew);
        assertEquals(MurmurHash.hash32(bs, bs.length), MurmurHash.hash32(ss, 2));
    }

    @Test
    public void testToByteArrayIntArrayInt() throws Exception {
        byte[] bs = new byte[8];
        bs[0] = 1;
        bs[1] = 2;
        bs[2] = 3;
        bs[3] = 4;
        bs[4] = 5;
        bs[5] = 6;
        bs[6] = 7;
        bs[7] = 8;
        int[] is = new int[4];
        is[0] = (4 << 0) | (3 << 8) | (2 << 16) | (1 << 24);
        is[1] = (8 << 0) | (7 << 8) | (6 << 16) | (5 << 24);

        System.out.printf("int: 0x%x\n", is[0]);
        System.out.printf("int: 0x%x\n", is[1]);
        
        byte[] bsNew = MurmurHash.toByteArray(is, 2);
        System.out.println("expected: " + Arrays.toString(bs));
        System.out.println("actual  : " + Arrays.toString(bsNew));
        Assert.assertArrayEquals(bs, bsNew);
        assertEquals(MurmurHash.hash32(bs, bs.length), MurmurHash.hash32(is, 2));
    }

    private static final long BYTE_MAX =  0xff;

    @Test
    public void testToByteArrayLongArrayInt() throws Exception {
        byte[] bs = new byte[16];
        bs[0] = 1;
        bs[1] = 2;
        bs[2] = 3;
        bs[3] = 4;
        bs[4] = 5;
        bs[5] = 6;
        bs[6] = 7;
        bs[7] = 8;
        bs[8] = 1;
        bs[9] = 2;
        bs[10] = 1;
        bs[11] = 4;
        bs[12] = 1;
        bs[13] = 6;
        bs[14] = 1;
        bs[15] = 8;
        long[] ls = new long[4];
        ls[0] = ((8 & BYTE_MAX) << 0) | ((7 & BYTE_MAX) << 8) | ((6 & BYTE_MAX) << 16) | ((5 & BYTE_MAX) << 24) 
                   | ((4 & BYTE_MAX) << 32) | ((3 & BYTE_MAX) << 40) | ((2 & BYTE_MAX) << 48) | ((1 & BYTE_MAX) << 56);
        ls[0] = ((8l) << 0) | ((7l) << 8) | ((6l) << 16) | ((5l) << 24) 
                | ((4l) << 32) | ((3l) << 40) | ((2l) << 48) | ((1l) << 56);
        ls[1] = (8l << 0) | (1l << 8) | (6l << 16) | (1l << 24) 
                | (4l << 32) | (1l << 40) | (2l << 48) | (1l << 56);

        System.out.printf("long: 0x%x\n", ls[0]);
        System.out.printf("long: 0x%x\n", ls[1]);
                
        byte[] bsNew = MurmurHash.toByteArray(ls, 2);
        System.out.println("expected: " + Arrays.toString(bs));
        System.out.println("actual  : " + Arrays.toString(bsNew));
        Assert.assertArrayEquals(bs, bsNew);
        assertEquals(MurmurHash.hash32(bs, bs.length), MurmurHash.hash32(ls, 2));
    }
    
}
