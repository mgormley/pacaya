package edu.jhu.util.matrix.infinite;

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

	public void decrement(int row, int k) {
		rows[row].decrement(k);
	}

	public int get(int row, int k) {
		return rows[row].get(k);
	}

	public int getNumRows() {
		return rows.length;
	}

	public void increment(int row, int k) {
		rows[row].increment(k);
	}

	public void decrement(int row, int k, int decr) {
		rows[row].decrement(k, decr);
	}

	public void increment(int row, int k, int incr) {
		rows[row].increment(k, incr);		
	}

}
