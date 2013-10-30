package edu.jhu.prim.matrix.infinite;

public class DenseInfiniteIntegerMatrix implements InfiniteIntegerMatrix {
    
	private InfiniteIntegerVector[] rows;

	public DenseInfiniteIntegerMatrix(int numRows) {
		this(numRows, 1);
	}    
	
	public DenseInfiniteIntegerMatrix(int numRows, int initialNumberOfColumns) {
		rows = new InfiniteIntegerVector[numRows];
		for (int row=0; row<rows.length; row++) {
			rows[row] = new DenseInfiniteIntegerVector(initialNumberOfColumns);
		}
	}

	public void decrement(int row, int col) {
		rows[row].decrement(col);
	}

	public int get(int row, int col) {
		return rows[row].get(col);
	}

	public int getNumRows() {
		return rows.length;
	}

	public void increment(int row, int col) {
		rows[row].increment(col);
	}

	public void decrement(int row, int col, int decr) {
		rows[row].decrement(col, decr);
	}

	public void increment(int row, int col, int incr) {
		rows[row].increment(col, incr);		
	}

}
