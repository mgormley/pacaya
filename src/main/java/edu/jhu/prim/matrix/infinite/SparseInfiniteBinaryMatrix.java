package edu.jhu.prim.matrix.infinite;

import java.util.LinkedList;

import edu.jhu.prim.vector.IntDoubleSortedVector;

public class SparseInfiniteBinaryMatrix implements InfiniteBinaryMatrix {

	private IntDoubleSortedVector[] matrix;
	private InfiniteIntegerVector columnCounts;
	private LinkedList<Integer> inactiveCols = new LinkedList<Integer>();

	private final int numRows;
	private int curMaxCol;

	/**
	 * An infinite number of columns is assumed.
	 */
	public SparseInfiniteBinaryMatrix(int numRows) {
		this(numRows, 1);
	}

	public SparseInfiniteBinaryMatrix(int numRows, int initialNumberOfColumns) {
		this.numRows = numRows;
		curMaxCol = initialNumberOfColumns;
		matrix = new IntDoubleSortedVector[numRows];
		for (int i = 0; i < numRows; i++) {
			matrix[i] = new IntDoubleSortedVector();
		}
		columnCounts = new DenseInfiniteIntegerVector(curMaxCol);
		for (int i = 0; i < curMaxCol; i++) {
			inactiveCols.add(i);
		}
	}

	public int getColumnCount(int col) {
		return columnCounts.get(col);
	}

	public int getNumRows() {
		return numRows;
	}

	public int getCurMaxColumn() {
		return curMaxCol;
	}

	public Iterable<Integer> getActiveColumns() {
		return columnCounts.getActiveIndices();
	}

	public boolean[][] getMatrix() {
		boolean[][] bmat = new boolean[numRows][curMaxCol];
		for (int i = 0; i < bmat.length; i++) {
			for (int col = 0; col < bmat[i].length; col++) {
				bmat[i][col] = (matrix[i].get(col) == 1.0);
			}
		}
		return bmat;
	}

	public boolean decrement(int row, int col) {
		checkIndices(row, col);

		// TODO: Speedup: Change SparseVector to return old value from set
		// this saves us the get(col).
		if (matrix[row].get(col) == 1.0) {
			columnCounts.decrement(col);
			if (columnCounts.get(col) == 0) {
				Integer colInteger = Integer.valueOf(col);
				inactiveCols.addFirst(colInteger);
			}
			matrix[row].set(col, 0.0);
			return true;
		}
		return false;
	}

	public boolean increment(int row, int col) {
		checkIndices(row, col);

		if (matrix[row].get(col) == 0.0) {
			columnCounts.increment(col);
			matrix[row].set(col, 1.0);
			return false;
		}
		return true;
	}

	public void incrementInactives(int row, int numInactives) {
		for (int i = 0; i < numInactives; i++) {
			incrementInactive(row);
		}
	}

	/**
	 * @return The index that was activated
	 */
	public int incrementInactive(int row) {
		checkRow(row);

		int k;
		if (inactiveCols.size() == 0) {
			k = curMaxCol++;
		} else {
			Integer kInteger = inactiveCols.removeFirst();
			k = kInteger.intValue();
		}
		increment(row, k);
		return k;
	}

	private void checkIndices(int row, int col) {
		checkRow(row);
		checkColumn(col);
	}

	private void checkColumn(int col) {
		if (col > curMaxCol) {
			throw new IllegalArgumentException(
					"Cannot change an inactive column: " + col);
		}
	}

	private void checkRow(int row) {
		if (row > numRows) {
			throw new IllegalArgumentException("Row exceeds numRows: " + row);
		}
	}

}
