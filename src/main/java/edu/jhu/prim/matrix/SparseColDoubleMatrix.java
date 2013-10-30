package edu.jhu.prim.matrix;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.vector.IntDoubleSortedVector;

public class SparseColDoubleMatrix implements DoubleMatrix {
    
    private static final long serialVersionUID = -2296616647180858488L;
    
    // Package private to give access to other matrix classes during multiplication.
    IntDoubleSortedVector[] cols;
	private final int numRows;
	private final int numCols;

	public SparseColDoubleMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		cols = new IntDoubleSortedVector[numCols];
		for (int col=0; col<numCols; col++) {
		    cols[col] = new IntDoubleSortedVector();
		}
	}

    public SparseColDoubleMatrix(double[][] matrix) {
        this(matrix.length, matrix[0].length);
        for (int row=0; row<matrix.length; row++) {
            assert(numCols == matrix[row].length);
        }
        
        for (int col=0; col<numCols; col++) {
            for (int row=0; row<numRows; row++) {
                if (matrix[row][col] != 0) {
                    // Set a non-zero value.
                    set(row, col, matrix[row][col]);
                }
            }
        }
    }
    
	public SparseColDoubleMatrix(SparseColDoubleMatrix other) {
	    this(other.numRows, other.numCols);
	    set(other);
    }

    public SparseColDoubleMatrix(int numRows, int numCols, int[] rowIndexes,
            int[] colIndexes, double[] values) {
        this(numRows, numCols);
        for (int i=0; i<rowIndexes.length; i++) {
            set(rowIndexes[i], colIndexes[i], values[i]);
        }
    }

    public void set(DoubleMatrix other) {
        if (other instanceof SparseColDoubleMatrix) {
            SparseColDoubleMatrix sim = (SparseColDoubleMatrix)other;
            assert(numRows == sim.numRows);
            assert(numCols == sim.numCols);
            for (int col=0; col<numCols; col++) {
                cols[col].set(sim.cols[col]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + other.getClass().getCanonicalName());
        }
    }

    public double get(int row, int col) {
        return cols[col].get(row);
    }
    
    public void set(int row, int col, double value) {
        checkDimensions(row, col);
        cols[col].set(row, value);
    }

	private void checkDimensions(int row, int col) {
        if (row < 0 || row >= numRows || col < 0 || col >= numCols) {
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid row or column. row=%d, col=%d, numRows=%d, numCols=%d",
                            row, col, numRows, numCols));
        }
    }

    public int getNumRows() {
        return numRows;
    }
	
	public int getNumColumns() {
        return numCols;
    }

	public void increment(int row, int col, double incr) {
        cols[col].add(row, incr);
	}

    public void decrement(int row, int col, double decr) {
        cols[col].add(row, -decr);
        assert(cols[col].get(row) >= 0);
    }

    public Iterable<IntDoubleEntry> getRowEntries(int row) {
        throw new RuntimeException("not implemented");
    }
    
    public Iterable<IntDoubleEntry> getColEntries(int col) {
        return cols[col];
    }

    public DoubleMatrix viewTranspose() {
        return new TransposeView(this);
    }
    
    /**
     * Multiplies A x B = C, where A is this matrix. 
     * 
     * @param bMat The matrix B.
     * @param transposeA Whether to transpose A prior to multiplication.
     * @param transposeB Whether to transpose B prior to multiplication.
     * @return The matrix C.
     */
    public DenseDoubleMatrix multT(DoubleMatrix bMat, boolean transposeA, boolean transposeB) {
        int numRowsC = transposeA ? this.getNumColumns() : this.getNumRows();
        int numColsC = transposeB ? bMat.getNumRows() : bMat.getNumColumns();
        DenseDoubleMatrix cMat = new DenseDoubleMatrix(numRowsC, numColsC);
        this.multT(bMat, cMat, transposeA, transposeB);
        return cMat;
    }
    
    /**
     * Multiplies A x B = C, where A is this matrix. 
     * 
     * @param bMat The matrix B.
     * @param cMat The matrix C.
     * @param transposeA Whether to transpose A prior to multiplication.
     * @param transposeB Whether to transpose B prior to multiplication.
     */
    public void multT(DoubleMatrix bMat, DoubleMatrix cMat, boolean transposeA, boolean transposeB) {
        if (!transposeA) {            
            throw new IllegalArgumentException("Multiplication where this matrix NOT transposed is not implemented.");
        }
        
        SparseRowDoubleMatrix.checkMultDimensions(this.viewTranspose(), transposeB ? bMat.viewTranspose() : bMat, cMat);

        int numRowsC = cMat.getNumRows();
        int numColsC = cMat.getNumColumns();
        
        if (bMat instanceof DenseDoubleMatrix) {
            DenseDoubleMatrix denseBMat = (DenseDoubleMatrix) bMat;
            for (int row = 0; row < numRowsC; row++) {
                for (int col = 0; col < numColsC; col++) {
                    if (transposeB) {
                        cMat.set(row, col, this.cols[row].dot(denseBMat.matrix[col]));
                    } else {
                        cMat.set(row, col, this.cols[row].dot(denseBMat.matrix, col));
                    }
                }
            }
        } else if (bMat instanceof SparseColDoubleMatrix && !transposeB) {
            SparseColDoubleMatrix sparseBMat = (SparseColDoubleMatrix) bMat;
            for (int row = 0; row < numRowsC; row++) {
                for (int col = 0; col < numColsC; col++) {
                    cMat.set(row, col, this.cols[row].dot(sparseBMat.cols[col]));
                }
            }
        } else if (bMat instanceof SparseRowDoubleMatrix && transposeB) {
            SparseRowDoubleMatrix sparseBMat = (SparseRowDoubleMatrix) bMat;
            for (int row = 0; row < numRowsC; row++) {
                for (int col = 0; col < numColsC; col++) {
                    cMat.set(row, col, this.cols[row].dot(sparseBMat.rows[col]));
                }
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + bMat.getClass().getCanonicalName());
        }
    }

}
