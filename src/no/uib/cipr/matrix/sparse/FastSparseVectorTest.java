package no.uib.cipr.matrix.sparse;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class FastSparseVectorTest {
    
    @Test
    public void testDotProduct() {
        SparseVector v1 = new FastSparseVector(Integer.MAX_VALUE);
        SparseVector v2 = new FastSparseVector(Integer.MAX_VALUE);

        v1.set(4, 530.8);
        v1.set(49, 2.3);
        v1.set(32, 2.2);
        v1.set(23, 10);

        v2.set(3, 20.4);
        v2.set(2, 1.1);
        v2.set(4, 1.1);
        v2.set(23, 2.4);
        v2.set(10, 0.001);
        v2.set(52, 1.1);
        v2.set(49, 7);

        assertEquals(1.1 * 530.8 + 10 * 2.4 + 2.3 * 7, v1.dot(v2), 1e-13);
    }

    @Test
    public void testInnerProduct() {
        FastSparseVector v1 = new FastSparseVector(Integer.MAX_VALUE);
        FastSparseVector v2 = new FastSparseVector(Integer.MAX_VALUE);

        v1.set(4, 530.8);
        v1.set(49, 2.3);
        v1.set(32, 2.2);
        v1.set(23, 10);

        v2.set(3, 20.4);
        v2.set(2, 1.1);
        v2.set(4, 1.1);
        v2.set(23, 2.4);
        v2.set(10, 0.001);
        v2.set(52, 1.1);
        v2.set(49, 7);

        SparseVector ip = v1.hadamardProd(v2);
        System.out.println(Arrays.toString(ip.getIndex()));
        System.out.println(Arrays.toString(ip.getData()));
        assertEquals(3, ip.getUsed());
        assertEquals(1.1 * 530.8, ip.get(4), 1e-13);
        assertEquals(10 * 2.4, ip.get(23), 1e-13);
        assertEquals(2.3 * 7, ip.get(49), 1e-13);
    }

}
