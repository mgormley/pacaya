package edu.jhu.util.matrix;

import java.util.Iterator;

import edu.jhu.util.Utilities;
import edu.jhu.util.vector.IntDoubleEntry;

public class DenseDoubleMatrix implements DoubleMatrix {
    
    private static final long serialVersionUID = -2148653126472159945L;
    
    private double[][] matrix;
	private final int numRows;
	private final int numCols;

	public DenseDoubleMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		matrix = new double[numRows][numCols];
	} 
	
	public DenseDoubleMatrix(double[][] matrix) {
        numRows = matrix.length;
        numCols = matrix[0].length;
        for (int row=0; row<matrix.length; row++) {
            assert(numCols == matrix[row].length);
        }
        this.matrix = matrix;        
    }
	
	public DenseDoubleMatrix(DenseDoubleMatrix dim) {
	    numRows = dim.numRows;
	    numCols = dim.numCols;
	    matrix = Utilities.copyOf(dim.matrix);
    }

    public void set(DoubleMatrix other) {
        if (other instanceof DenseDoubleMatrix) {
            DenseDoubleMatrix dim = (DenseDoubleMatrix)other;
            assert(numRows == dim.numRows);
            assert(numCols == dim.numCols);
            for (int row=0; row<numRows; row++) {
                Utilities.copy(dim.matrix[row], matrix[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + other.getClass().getCanonicalName());
        }
    }
    
    public void decrement(int row, int col) {
		matrix[row][col]--;
		assert(matrix[row][col] >= 0);
	}

	public double get(int row, int col) {
		return matrix[row][col];
	}
	
    public void set(int row, int col, double value) {
        matrix[row][col] = value;
    }

	public int getNumRows() {
        return numRows;
    }
	
	public int getNumColumns() {
        return numCols;
    }

    public double[][] getMatrix() {
        return matrix;
    }
    
	public void decrement(int row, int col, double decr) {
	    matrix[row][col] -= decr;
	    assert(matrix[row][col] >= 0);
	}

	public void increment(int row, int col, double incr) {
	    matrix[row][col] += incr;
	}
	
	public void viewTranspose() {
	    //return new TransposeView()
	}

    @Override
    public Iterable<IntDoubleEntry> getRowEntries(int row) {
        return new DenseDoubleVectorIterator(matrix[row]);
    }
    
    /**
     * Iterator over a sparse vector
     */
    private static class DenseDoubleVectorIterator implements Iterator<IntDoubleEntry>, Iterable<IntDoubleEntry> {

        private int cursor;
        private double[] row;
        
        public DenseDoubleVectorIterator(double[] row) {
            cursor = 0;
            this.row = row;
        }

        private final DenseIntDoubleEntry entry = new DenseIntDoubleEntry();

        public boolean hasNext() {
            return cursor < row.length;
        }

        public IntDoubleEntry next() {
            entry.update(cursor);
            
            cursor++;
            while(cursor < row.length && row[cursor] == 0) {
                cursor++;
            }

            return entry;
        }

        public void remove() {
            throw new RuntimeException("not implemented");
        }

        @Override
        public Iterator<IntDoubleEntry> iterator() {
            return this;
        }
        
        /**
         * Entry of a dense vector
         */
        private class DenseIntDoubleEntry implements IntDoubleEntry {

            private int cursor = 0;

            public void update(int cursor) {
                this.cursor = cursor;
            }

            public int index() {
                return cursor;
            }

            public double get() {
                return row[cursor];
            }

            public void set(double value) {
                row[cursor] = value;
            }

        }

    }    

}
