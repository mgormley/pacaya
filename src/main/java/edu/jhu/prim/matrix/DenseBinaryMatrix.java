package edu.jhu.prim.matrix;

import edu.jhu.prim.matrix.infinite.ColumnCountIterator;

public class DenseBinaryMatrix implements BinaryMatrix {

    private static final long serialVersionUID = -6512663639870324357L;
    private boolean[][] matrix;
    private int[] columnCounts;
    
    private final int numRows;
    private final int numCols;

    /**
     * An infinite number of columns is assumed.
     */
    public DenseBinaryMatrix(int numRows, int numColumns) {
        this.numRows = numRows;
        this.numCols = numColumns;
        matrix = new boolean[numRows][numColumns];
        columnCounts = new int[numCols];
    }

    public int getColumnCount(int col) {
        return columnCounts[col];
    }

    public int getNumRows() {
        return numRows;
    }

    public int getNumColumns() {
        return numCols;
    }

    public Iterable<Integer> getActiveColumns() {
        return new ColumnCountIterator(columnCounts);
    }

    public boolean[][] getMatrix() {
        return matrix;
    }

    public boolean decrement(int row, int col) {
        if (matrix[row][col]) {
            columnCounts[col]--;
            matrix[row][col] = false;
            return true;
        }
        return false;
    }

    public boolean increment(int row, int k) {
        if (!matrix[row][k]) {
            columnCounts[k]++;
            matrix[row][k] = true;
            return false;
        }
        return true;
    }

	public boolean get(int row, int col) {
		return matrix[row][col];
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean[][] matrix = getMatrix();
        for (int i=0; i<matrix.length; i++) {
        	for (int j=0; j<matrix[i].length; j++) {
        		if (matrix[i][j]) {
        			sb.append("1");
        		} else {
        			sb.append("0");
        		}
        	}
        	sb.append("\n");
        }
		return sb.toString();
	}

    public int getRowCount(int row) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void set(BinaryMatrix matrix) {
        throw new RuntimeException("not implemented");
    }

}
