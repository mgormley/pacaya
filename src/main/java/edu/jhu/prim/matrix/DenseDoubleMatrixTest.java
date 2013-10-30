package edu.jhu.prim.matrix;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;

public class DenseDoubleMatrixTest {

    @Test
    public void testMultWithDense() {
        // 4 x 2 matrix.
        double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 3 matrix.
        double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 4 x 3 matrix.
        double[][] cMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        DenseDoubleMatrix aMat = new DenseDoubleMatrix(aMatArr);
        DenseDoubleMatrix bMat = new DenseDoubleMatrix(bMatArr);
                
        DenseDoubleMatrix cMat = aMat.mult(bMat);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()));
        JUnitUtils.assertArrayEquals(cMatArr, cMat.getMatrix(), 1e-13);
    }    

    @Test
    public void testMultWithSparseCol() {
        // 4 x 2 matrix.
        double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 3 matrix.
        double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 4 x 3 matrix.
        double[][] cMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        DenseDoubleMatrix aMat = new DenseDoubleMatrix(aMatArr);
        SparseColDoubleMatrix bMat = new SparseColDoubleMatrix(bMatArr);
                
        DenseDoubleMatrix cMat = aMat.mult(bMat);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()));
        JUnitUtils.assertArrayEquals(cMatArr, cMat.getMatrix(), 1e-13);
    }

}
