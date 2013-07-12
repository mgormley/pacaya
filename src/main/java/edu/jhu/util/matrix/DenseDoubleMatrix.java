package edu.jhu.util.matrix;

import java.util.Iterator;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Utilities;

public class DenseDoubleMatrix implements DoubleMatrix {
    
    private static final long serialVersionUID = -2148653126472159945L;
    
    // Package private to give access to other matrix classes during multiplication.
    double[][] matrix;
	final int numRows;
	final int numCols;

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
            // This is the standard slow implementation of dense matrix multiplication. Nothing fancy.            
            DenseDoubleMatrix denseBMat = (DenseDoubleMatrix) bMat;
            for (int row = 0; row < cMat.numRows; row++) {
                for (int col = 0; col < cMat.numCols; col++) {
                    // Compute the dot product of the row in A with the column in B.
                    double value = 0.0;
                    for (int i=0; i<matrix[row].length; i++) {
                        value += matrix[row][i] * denseBMat.matrix[i][col];
                    }
                    cMat.matrix[row][col] = value;
                }
            }
        } else if (bMat instanceof SparseColDoubleMatrix) {
            SparseColDoubleMatrix sparseBMat = (SparseColDoubleMatrix) bMat;
            for (int row = 0; row < cMat.numRows; row++) {
                for (int col = 0; col < cMat.numCols; col++) {
                    cMat.matrix[row][col] = sparseBMat.cols[col].dot(this.matrix[row]);
                }
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + bMat.getClass().getCanonicalName());
        }
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
