package edu.jhu.util.matrix;

import edu.jhu.util.vector.IntIntEntry;
import edu.jhu.util.vector.SortedIntIntVector;

public class SparseIntegerMatrix implements IntegerMatrix {
    
    private static final long serialVersionUID = -2296616647180858488L;
    
    private SortedIntIntVector[] matrix;
	private final int numRows;
	private final int numCols;

	public SparseIntegerMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		matrix = new SortedIntIntVector[numRows];
		for (int row=0; row<numRows; row++) {
		    matrix[row] = new SortedIntIntVector();
		}
	}
	
	public SparseIntegerMatrix(SparseIntegerMatrix dim) {
	    this(dim.numRows, dim.numCols);
	    set(dim);
    }

    public void set(IntegerMatrix im) {
        if (im instanceof SparseIntegerMatrix) {
            SparseIntegerMatrix sim = (SparseIntegerMatrix)im;
            assert(numRows == sim.numRows);
            assert(numCols == sim.numCols);
            for (int row=0; row<numRows; row++) {
                matrix[row].set(sim.matrix[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + im.getClass().getCanonicalName());
        }
    }

	public int get(int row, int col) {
		return matrix[row].get(col);
	}

	public int getNumRows() {
        return numRows;
    }
	
	public int getNumColumns() {
        return numCols;
    }

	public void increment(int row, int col) {
	    matrix[row].add(col, 1);
	}

    public void decrement(int row, int col) {
        matrix[row].add(col, -1);
        assert(matrix[row].get(col) >= 0);
    }

	public void increment(int row, int col, int incr) {
        matrix[row].add(col, incr);
	}

    public void decrement(int row, int col, int decr) {
        matrix[row].add(col, -decr);
        assert(matrix[row].get(col) >= 0);
    }
    
    public Iterable<IntIntEntry> getRowEntries(int row) {
        return matrix[row];
    }

}
