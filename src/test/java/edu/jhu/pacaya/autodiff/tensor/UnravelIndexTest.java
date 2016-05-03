package edu.jhu.pacaya.autodiff.tensor;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.Tensor;

public class UnravelIndexTest {

    private void auxTestIndex(int configIx, int[] dims, int... expected) {
        org.junit.Assert.assertArrayEquals(expected, Tensor.unravelIndex(configIx, dims));
        org.junit.Assert.assertEquals(configIx, Tensor.ravelIndex(expected, dims));
    }

    private void auxTestMatlabIndex(int configIx, int[] dims, int... expected) {
        ArrayUtils.toString(expected);
        int[] observed = Tensor.unravelIndexMatlab(configIx, dims);
        System.out.println(String.format("expected: %s\nobserved: %s", ArrayUtils.toString(expected), ArrayUtils.toString(observed)));
        org.junit.Assert.assertArrayEquals(expected, observed);
        org.junit.Assert.assertEquals(configIx, Tensor.ravelIndexMatlab(expected, dims));
    }
    
    private void brokenVarArgsSwap(int... ints) {
        ints = ints.clone();
        int tmp = ints[0];
        ints[0] = ints[1];
        ints[1] = tmp;
    }
    
    @Test
    public void testBrokenSwap() {
        int[] orig = { 1, 2 };  
        int[] intsA = { 1, 2 };  
        brokenVarArgsSwap(intsA);
        org.junit.Assert.assertArrayEquals(orig, intsA);
        int[] intsB = new int[2];
        intsB[0] = 1;
        intsB[1] = 2;
        brokenVarArgsSwap(intsB);
        org.junit.Assert.assertArrayEquals(orig, intsB);
    }
    
    @Test
    public void testUnravelIndex() {
        int[] dimsEasy = { 1, 1 };
        auxTestIndex(0, dimsEasy, 0, 0);

        int[] dimsEasy2 = { 2, 2 };
        auxTestIndex(0, dimsEasy2, 0, 0);
        auxTestIndex(1, dimsEasy2, 0, 1);
        auxTestIndex(2, dimsEasy2, 1, 0);
        auxTestIndex(3, dimsEasy2, 1, 1);

        int[] dimsEasy3 = { 1, 2, 3 };
        auxTestIndex(0, dimsEasy3, 0, 0, 0);
        auxTestIndex(1, dimsEasy3, 0, 0, 1);
        auxTestIndex(2, dimsEasy3, 0, 0, 2);
        auxTestIndex(3, dimsEasy3, 0, 1, 0);
        auxTestIndex(4, dimsEasy3, 0, 1, 1);
        auxTestIndex(5, dimsEasy3, 0, 1, 2);

        int[] dimsEasy4 = { 4, 1, 2, 3 };
        auxTestIndex(0, dimsEasy4, 0, 0, 0, 0);
        auxTestIndex(1, dimsEasy4, 0, 0, 0, 1);
        auxTestIndex(2, dimsEasy4, 0, 0, 0, 2);
        auxTestIndex(3, dimsEasy4, 0, 0, 1, 0);
        auxTestIndex(4, dimsEasy4, 0, 0, 1, 1);
        auxTestIndex(5, dimsEasy4, 0, 0, 1, 2);
        auxTestIndex(6, dimsEasy4, 1, 0, 0, 0);
        auxTestIndex(7, dimsEasy4, 1, 0, 0, 1);
        auxTestIndex(8, dimsEasy4, 1, 0, 0, 2);
        auxTestIndex(9, dimsEasy4, 1, 0, 1, 0);
        auxTestIndex(10, dimsEasy4, 1, 0, 1, 1);
        auxTestIndex(11, dimsEasy4, 1, 0, 1, 2);
        auxTestIndex(12, dimsEasy4, 2, 0, 0, 0);
        auxTestIndex(13, dimsEasy4, 2, 0, 0, 1);
        auxTestIndex(14, dimsEasy4, 2, 0, 0, 2);
        auxTestIndex(15, dimsEasy4, 2, 0, 1, 0);
        auxTestIndex(16, dimsEasy4, 2, 0, 1, 1);
        auxTestIndex(17, dimsEasy4, 2, 0, 1, 2);
        auxTestIndex(18, dimsEasy4, 3, 0, 0, 0);
        auxTestIndex(19, dimsEasy4, 3, 0, 0, 1);
        auxTestIndex(20, dimsEasy4, 3, 0, 0, 2);
        auxTestIndex(21, dimsEasy4, 3, 0, 1, 0);
        auxTestIndex(22, dimsEasy4, 3, 0, 1, 1);
        auxTestIndex(23, dimsEasy4, 3, 0, 1, 2);

        auxTestMatlabIndex(0,  dimsEasy4, 0, 0, 0, 0);
        auxTestMatlabIndex(1,  dimsEasy4, 1, 0, 0, 0);
        auxTestMatlabIndex(2,  dimsEasy4, 2, 0, 0, 0);
        auxTestMatlabIndex(3,  dimsEasy4, 3, 0, 0, 0);
        auxTestMatlabIndex(4,  dimsEasy4, 0, 0, 1, 0);
        auxTestMatlabIndex(5,  dimsEasy4, 1, 0, 1, 0);
        auxTestMatlabIndex(6,  dimsEasy4, 2, 0, 1, 0);
        auxTestMatlabIndex(7,  dimsEasy4, 3, 0, 1, 0);
        auxTestMatlabIndex(8,  dimsEasy4, 0, 0, 0, 1);
        auxTestMatlabIndex(9,  dimsEasy4, 1, 0, 0, 1);
        auxTestMatlabIndex(10, dimsEasy4, 2, 0, 0, 1);
        auxTestMatlabIndex(11, dimsEasy4, 3, 0, 0, 1);
        auxTestMatlabIndex(12, dimsEasy4, 0, 0, 1, 1);
        auxTestMatlabIndex(13, dimsEasy4, 1, 0, 1, 1);
        auxTestMatlabIndex(14, dimsEasy4, 2, 0, 1, 1);
        auxTestMatlabIndex(15, dimsEasy4, 3, 0, 1, 1);
        auxTestMatlabIndex(16, dimsEasy4, 0, 0, 0, 2);
        auxTestMatlabIndex(17, dimsEasy4, 1, 0, 0, 2);
        auxTestMatlabIndex(18, dimsEasy4, 2, 0, 0, 2);
        auxTestMatlabIndex(19, dimsEasy4, 3, 0, 0, 2);
        auxTestMatlabIndex(20, dimsEasy4, 0, 0, 1, 2);
        auxTestMatlabIndex(21, dimsEasy4, 1, 0, 1, 2);
        auxTestMatlabIndex(22, dimsEasy4, 2, 0, 1, 2);
        auxTestMatlabIndex(23, dimsEasy4, 3, 0, 1, 2);
    }

}
