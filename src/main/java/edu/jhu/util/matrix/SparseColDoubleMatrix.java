package edu.jhu.util.matrix;

import edu.jhu.util.vector.IntDoubleEntry;
import edu.jhu.util.vector.SortedIntDoubleVector;

public class SparseColDoubleMatrix implements DoubleMatrix {
    
    private static final long serialVersionUID = -2296616647180858488L;
    
    private SortedIntDoubleVector[] cols;
	private final int numRows;
	private final int numCols;

	public SparseColDoubleMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		cols = new SortedIntDoubleVector[numCols];
		for (int col=0; col<numCols; col++) {
		    cols[col] = new SortedIntDoubleVector();
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

    public void mult(DenseDoubleMatrix bMat, DenseDoubleMatrix cMat) {
        // TODO:  
        
    }
}
