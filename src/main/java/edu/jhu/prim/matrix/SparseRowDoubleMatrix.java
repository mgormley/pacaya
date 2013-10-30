package edu.jhu.prim.matrix;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.prim.vector.IntDoubleSortedVector;

public class SparseRowDoubleMatrix implements DoubleMatrix {
    
    private static final long serialVersionUID = -2296616647180858488L;
    
    // Package private to give access to other matrix classes during multiplication.
    IntDoubleSortedVector[] rows;
	private final int numRows;
	private final int numCols;

	public SparseRowDoubleMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		rows = new IntDoubleSortedVector[numRows];
		for (int row=0; row<numRows; row++) {
		    rows[row] = new IntDoubleSortedVector();
		}
	}

    public SparseRowDoubleMatrix(double[][] matrix) {
        this(matrix.length, matrix[0].length);
        for (int row=0; row<matrix.length; row++) {
            assert(numCols == matrix[row].length);
        }
        
        for (int row=0; row<matrix.length; row++) {
            for (int col=0; col<matrix[row].length; col++) {
                if (matrix[row][col] != 0) {
                    // Set a non-zero value.
                    set(row, col, matrix[row][col]);
                }
            }
        }
    }
    
	public SparseRowDoubleMatrix(SparseRowDoubleMatrix other) {
	    this(other.numRows, other.numCols);
	    set(other);
    }

    public void set(DoubleMatrix other) {
        if (other instanceof SparseRowDoubleMatrix) {
            SparseRowDoubleMatrix sim = (SparseRowDoubleMatrix)other;
            assert(numRows == sim.numRows);
            assert(numCols == sim.numCols);
            for (int row=0; row<numRows; row++) {
                rows[row].set(sim.rows[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + other.getClass().getCanonicalName());
        }
    }

    public double get(int row, int col) {
        return rows[row].get(col);
    }
    
    public void set(int row, int col, double value) {
        checkDimensions(row, col);
        rows[row].set(col, value);
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

	public void increment(int row, int col) {
	    rows[row].add(col, 1);
	}

    public void decrement(int row, int col) {
        rows[row].add(col, -1);
        assert(rows[row].get(col) >= 0);
    }

	public void increment(int row, int col, double incr) {
        rows[row].add(col, incr);
	}

    public void decrement(int row, int col, double decr) {
        rows[row].add(col, -decr);
        assert(rows[row].get(col) >= 0);
    }
    
    public Iterable<IntDoubleEntry> getRowEntries(int row) {
        return rows[row];
    }

    public DoubleMatrix viewTranspose() {
        return new TransposeView(this);
    }
    
    public DenseDoubleMatrix mult(DoubleMatrix bMat) {
        DenseDoubleMatrix cMat = new DenseDoubleMatrix(this.getNumRows(), bMat.getNumColumns());
        this.mult(bMat, cMat);
        return cMat;
    }
    
    public void mult(DoubleMatrix bMat, DenseDoubleMatrix cMat) {
        SparseRowDoubleMatrix.checkMultDimensions(this, bMat, cMat);

        if (bMat instanceof DenseDoubleMatrix) {
            DenseDoubleMatrix denseBMat = (DenseDoubleMatrix) bMat;
            for (int row = 0; row < cMat.numRows; row++) {
                for (int col = 0; col < cMat.numCols; col++) {
                    cMat.matrix[row][col] = this.rows[row].dot(denseBMat.matrix, col);
                }
            }
        } else if (bMat instanceof SparseColDoubleMatrix) {
            SparseColDoubleMatrix sparseBMat = (SparseColDoubleMatrix) bMat;
            for (int row = 0; row < cMat.numRows; row++) {
                for (int col = 0; col < cMat.numCols; col++) {
                    cMat.matrix[row][col] = this.rows[row].dot(sparseBMat.cols[col]);
                }
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + bMat.getClass().getCanonicalName());
        }
    }

    public static void checkMultDimensions(
            DoubleMatrix aMat,
            DoubleMatrix bMat, DoubleMatrix cMat) {
        if (aMat.getNumColumns() != bMat.getNumRows() || aMat.getNumRows() != cMat.getNumRows() || bMat.getNumColumns() != cMat.getNumColumns()) {
            throw new IllegalArgumentException("Invalid matrix dimensions for multiplication.");
        }
    }

    
}
