package edu.jhu.prim.matrix;

import edu.jhu.prim.map.IntIntEntry;
import edu.jhu.prim.vector.IntIntSortedVector;

public class SparseIntegerMatrix implements IntegerMatrix {
    
    private static final long serialVersionUID = -2296616647180858488L;
    
    private IntIntSortedVector[] rows;
	private final int numRows;
	private final int numCols;

	public SparseIntegerMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		rows = new IntIntSortedVector[numRows];
		for (int row=0; row<numRows; row++) {
		    rows[row] = new IntIntSortedVector();
		}
	}

    public SparseIntegerMatrix(int[][] matrix) {
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
                rows[row].set(sim.rows[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + im.getClass().getCanonicalName());
        }
    }

    public int get(int row, int col) {
        return rows[row].get(col);
    }
    
    public void set(int row, int col, int value) {
        rows[row].set(col, value);
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

	public void increment(int row, int col, int incr) {
        rows[row].add(col, incr);
	}

    public void decrement(int row, int col, int decr) {
        rows[row].add(col, -decr);
        assert(rows[row].get(col) >= 0);
    }
    
    public Iterable<IntIntEntry> getRowEntries(int row) {
        return rows[row];
    }

}
