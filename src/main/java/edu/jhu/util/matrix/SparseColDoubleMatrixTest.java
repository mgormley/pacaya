package edu.jhu.util.matrix;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;

public class SparseColDoubleMatrixTest {

    @Test
    public void testMultTWithDense() {
        // 4 x 2 matrix.
        //double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 4 matrix.
        double[][] atMatArr = new double[][]{ {2, 5, 11, 17}, {3, 7, 13, 19} };
        // 2 x 3 matrix.
        double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 4 x 3 matrix.
        double[][] abMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        SparseColDoubleMatrix aMat = new SparseColDoubleMatrix(atMatArr);
        DenseDoubleMatrix bMat = new DenseDoubleMatrix(bMatArr);
                
        DenseDoubleMatrix cMat = aMat.multT(bMat, true, false);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()).replace('[', '{').replace(']', '}'));
        JUnitUtils.assertArrayEquals(abMatArr, cMat.getMatrix(), 1e-13);
    }
    
    @Test
    public void testMultTWithDenseT() {
        // 4 x 2 matrix.
        //double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 4 matrix.
        double[][] atMatArr = new double[][]{ {2, 5, 11, 17}, {3, 7, 13, 19} };
        // 2 x 3 matrix.
        //double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 3 x 2 matrix.
        double[][] btMatArr = new double[][]{ {2, 7}, {3, 11}, {5, 13} };
        // 4 x 3 matrix.
        double[][] abMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        SparseColDoubleMatrix aMat = new SparseColDoubleMatrix(atMatArr);
        DenseDoubleMatrix bMat = new DenseDoubleMatrix(btMatArr);
                
        DenseDoubleMatrix cMat = aMat.multT(bMat, true, true);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()).replace('[', '{').replace(']', '}'));
        JUnitUtils.assertArrayEquals(abMatArr, cMat.getMatrix(), 1e-13);
    }

    @Test
    public void testMultTWithSparseCol() {
        // 4 x 2 matrix.
        //double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 4 matrix.
        double[][] atMatArr = new double[][]{ {2, 5, 11, 17}, {3, 7, 13, 19} };
        // 2 x 3 matrix.
        double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 4 x 3 matrix.
        double[][] abMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        SparseColDoubleMatrix aMat = new SparseColDoubleMatrix(atMatArr);
        SparseColDoubleMatrix bMat = new SparseColDoubleMatrix(bMatArr);
                
        DenseDoubleMatrix cMat = aMat.multT(bMat, true, false);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()).replace('[', '{').replace(']', '}'));
        JUnitUtils.assertArrayEquals(abMatArr, cMat.getMatrix(), 1e-13);
    }
    
    @Test
    public void testMultTWithSparseRowT() {
        // 4 x 2 matrix.
        //double[][] aMatArr = new double[][]{ {2, 3}, {5, 7}, {11, 13}, {17, 19} };
        // 2 x 4 matrix.
        double[][] atMatArr = new double[][]{ {2, 5, 11, 17}, {3, 7, 13, 19} };
        // 2 x 3 matrix.
        //double[][] bMatArr = new double[][]{ {2, 3, 5}, {7, 11, 13} };
        // 3 x 2 matrix.
        double[][] btMatArr = new double[][]{ {2, 7}, {3, 11}, {5, 13} };
        // 4 x 3 matrix.
        double[][] abMatArr = new double[][]{{25, 39, 49}, {59, 92, 116}, {113, 176, 224}, {167, 260, 332}};

        
        SparseColDoubleMatrix aMat = new SparseColDoubleMatrix(atMatArr);
        SparseRowDoubleMatrix bMat = new SparseRowDoubleMatrix(btMatArr);
                
        DenseDoubleMatrix cMat = aMat.multT(bMat, true, true);
        
        System.out.println(Arrays.deepToString(cMat.getMatrix()).replace('[', '{').replace(']', '}'));
        JUnitUtils.assertArrayEquals(abMatArr, cMat.getMatrix(), 1e-13);
    }


}
