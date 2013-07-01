package edu.jhu.util.matrix;

import edu.jhu.util.Utilities;
import edu.jhu.util.vector.IntIntEntry;

public class DenseIntegerMatrixWithCounts implements IntegerMatrix {
    
    private static final long serialVersionUID = -2148653126472159945L;
    
    private int[][] matrix;
	private final int numRows;
	private final int numCols;
    private int[] colCounts;
    private int[] rowCounts;
    
	public DenseIntegerMatrixWithCounts(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		matrix = new int[numRows][numCols];
		colCounts = new int[numCols];
		rowCounts = new int[numRows];
	} 
	
	public DenseIntegerMatrixWithCounts(DenseIntegerMatrixWithCounts dim) {
	    numRows = dim.numRows;
	    numCols = dim.numCols;
	    matrix = Utilities.copyOf(dim.matrix);
	    colCounts = Utilities.copyOf(dim.colCounts, dim.colCounts.length);
	    rowCounts = Utilities.copyOf(dim.rowCounts, dim.rowCounts.length);
    }
	
	public void set(IntegerMatrix other) {
        if (other instanceof DenseIntegerMatrixWithCounts) {
            DenseIntegerMatrixWithCounts dim = (DenseIntegerMatrixWithCounts)other;
            assert(numRows == dim.numRows);
            assert(numCols == dim.numCols);
            Utilities.copy(dim.colCounts, colCounts);
            Utilities.copy(dim.rowCounts, rowCounts);
            for (int row=0; row<numRows; row++) {
                Utilities.copy(dim.matrix[row], matrix[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + other.getClass().getCanonicalName());
        }
    }

    public void decrement(int row, int col) {
		matrix[row][col]--;
		colCounts[col]--;
		rowCounts[row]--;
		assert(matrix[row][col] >= 0);
	    assert(rowCounts[row] >= 0);
	}

	public int get(int row, int col) {
		return matrix[row][col];
	}

    public int getColumnCount(int col) {
        return colCounts[col];
    }

    public int getRowCount(int row) {
        return rowCounts[row];
    }
    
	public int getNumRows() {
        return numRows;
    }
	
	public int getNumColumns() {
        return numCols;
    }

	public void increment(int row, int k) {
		matrix[row][k]++;
		colCounts[k]++;
		rowCounts[row]++;
	}

	public void decrement(int row, int k, int decr) {
	    matrix[row][k] -= decr;
	    colCounts[k] -= decr;
	    rowCounts[row] -= decr;
	    assert(matrix[row][k] >= 0);
	    assert(rowCounts[row] >= 0);
	}

	public void increment(int row, int k, int incr) {
	    matrix[row][k] += incr;
	    colCounts[k] += incr;
	    rowCounts[row] += incr;
	}

    @Override
    public Iterable<IntIntEntry> getRowEntries(int row) {
        throw new RuntimeException("not implemented");
    }

}
