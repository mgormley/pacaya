package edu.jhu.util.matrix;

import java.util.Iterator;

import edu.jhu.util.Utilities;
import edu.jhu.util.vector.IntIntEntry;

public class DenseIntegerMatrix implements IntegerMatrix {
    
    private static final long serialVersionUID = -2148653126472159945L;
    
    private int[][] matrix;
	private final int numRows;
	private final int numCols;

	public DenseIntegerMatrix(int numRows, int numCols) {
	    this.numRows = numRows;
	    this.numCols = numCols;
		matrix = new int[numRows][numCols];
	} 
	
	public DenseIntegerMatrix(DenseIntegerMatrix dim) {
	    numRows = dim.numRows;
	    numCols = dim.numCols;
	    matrix = Utilities.copyOf(dim.matrix);
    }

    public void set(IntegerMatrix im) {
        if (im instanceof DenseIntegerMatrix) {
            DenseIntegerMatrix dim = (DenseIntegerMatrix)im;
            assert(numRows == dim.numRows);
            assert(numCols == dim.numCols);
            for (int row=0; row<numRows; row++) {
                Utilities.copy(dim.matrix[row], matrix[row]);
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + im.getClass().getCanonicalName());
        }
    }
    
    public void decrement(int row, int k) {
		matrix[row][k]--;
		assert(matrix[row][k] >= 0);
	}

	public int get(int row, int k) {
		return matrix[row][k];
	}

	public int getNumRows() {
        return numRows;
    }
	
	public int getNumColumns() {
        return numCols;
    }

    public int[][] getMatrix() {
        return matrix;
    }
    
	public void increment(int row, int k) {
		matrix[row][k]++;
	}

	public void decrement(int row, int k, int decr) {
	    matrix[row][k] -= decr;
	    assert(matrix[row][k] >= 0);
	}

	public void increment(int row, int k, int incr) {
	    matrix[row][k] += incr;
	}

    @Override
    public Iterable<IntIntEntry> getRowEntries(int row) {
        return new DenseIntVectorIterator(matrix[row]);
    }
    
    /**
     * Iterator over a sparse vector
     */
    private static class DenseIntVectorIterator implements Iterator<IntIntEntry>, Iterable<IntIntEntry> {

        private int cursor;
        private int[] row;
        
        public DenseIntVectorIterator(int[] row) {
            cursor = 0;
            this.row = row;
        }

        private final DenseIntIntEntry entry = new DenseIntIntEntry();

        public boolean hasNext() {
            return cursor < row.length;
        }

        public IntIntEntry next() {
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
        public Iterator<IntIntEntry> iterator() {
            return this;
        }
        
        /**
         * Entry of a dense vector
         */
        private class DenseIntIntEntry implements IntIntEntry {

            private int cursor = 0;

            public void update(int cursor) {
                this.cursor = cursor;
            }

            public int index() {
                return cursor;
            }

            public int get() {
                return row[cursor];
            }

            public void set(int value) {
                row[cursor] = value;
            }

        }

    }

    

}
