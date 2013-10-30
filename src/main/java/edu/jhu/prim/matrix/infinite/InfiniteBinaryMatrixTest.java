package edu.jhu.prim.matrix.infinite;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;



public class InfiniteBinaryMatrixTest {

	@Test
	public void testDenseInfiniteBinaryMatrix() {
	    System.out.println("DenseInfiniteBinaryMatrix");
		DenseInfiniteBinaryMatrix matrix = new DenseInfiniteBinaryMatrix(3);
		testMatrix(matrix);
	}
	
	@Test
    public void testSparseInfiniteBinaryMatrix() {
	    System.out.println("SparseInfiniteBinaryMatrix");
        SparseInfiniteBinaryMatrix matrix = new SparseInfiniteBinaryMatrix(3);
        testMatrix(matrix);
    }
	
	@Test
    public void testBitSetInfiniteBinaryMatrix() {
        System.out.println("BitSetInfiniteBinaryMatrix");
        BitSetInfiniteBinaryMatrix matrix = new BitSetInfiniteBinaryMatrix(3);
        testMatrix(matrix);
    }

    private void testMatrix(InfiniteBinaryMatrix matrix) {
        matrix.incrementInactives(0, 3);
		matrix.increment(1, 1);
		matrix.incrementInactives(1, 3);
		matrix.incrementInactives(2, 1);
		matrix.decrement(1, 4);
		matrix.incrementInactives(2, 2);
		
		boolean[][] expected = new boolean[][] {
			new boolean[]{true, true, true,false,false,false,false,false},
			new boolean[]{false,true,false,true,false,true,false,false},
			new boolean[]{false,false,false,false,true,false,true,true},
		};
		boolean[][] result = matrix.getMatrix();
		
		System.out.println("expected:");
		displayMatrix(expected);
		System.out.println("matrix:");
		displayMatrix(result);
		
		assertEquals(expected.length, result.length);
		for (int i=0; i<expected.length; i++) {
			assertTrue(Arrays.equals(expected[i],result[i]));
		}
		
		// Test column counts
		int[] expectedColumns = new int[]{1,2,1,1,1,1,1,1};
		assertEquals(expectedColumns.length, matrix.getCurMaxColumn());
		for (Integer k : matrix.getActiveColumns()) {
		    assertEquals(expectedColumns[k], matrix.getColumnCount(k));
		}
    }

	public static void displayMatrix(InfiniteBinaryMatrix imatrix) {
	    displayMatrix(imatrix.getMatrix());
	}

    public static void displayMatrix(boolean[][] matrix) {
        for (int i=0; i<matrix.length; i++) {
        	for (int j=0; j<matrix[i].length; j++) {
        		if (matrix[i][j]) {
        			System.out.print(1);
        		} else {
        			System.out.print(0);
        		}
        	}
        	System.out.println();
        }
    }
	
}
