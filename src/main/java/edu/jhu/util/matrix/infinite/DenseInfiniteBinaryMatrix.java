package edu.jhu.util.matrix.infinite;

import java.util.Arrays;
import java.util.LinkedList;

import edu.jhu.util.Utilities;

public class DenseInfiniteBinaryMatrix implements InfiniteBinaryMatrix {

    private boolean[][] matrix;
    private InfiniteIntegerVector columnCounts;
    private LinkedList<Integer> inactiveCols = new LinkedList<Integer>();
    
    private final int numRows;
    private int curMaxCol;

    /**
     * An infinite number of columns is assumed.
     */
    public DenseInfiniteBinaryMatrix(int numRows) {
        this(numRows, 1);
    }
    
    public DenseInfiniteBinaryMatrix(int numRows, int initialNumberOfColumns) {
    	this.numRows = numRows;
        curMaxCol = initialNumberOfColumns;
        matrix = new boolean[numRows][curMaxCol];
        for (int i=0; i<numRows; i++) {
            Arrays.fill(matrix[i], false);
        }
        columnCounts = new DenseInfiniteIntegerVector(curMaxCol);
        for (int i=0; i<curMaxCol; i++) {
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
		return matrix;
	}
	
    public boolean decrement(int row, int col) {
        checkIndices(row, col);
        
        if (matrix[row][col]) {
            columnCounts.decrement(col); 
            if (columnCounts.get(col) == 0) {
                Integer kInteger = Integer.valueOf(col);
                inactiveCols.addFirst(kInteger);
            }
            matrix[row][col] = false;
            return true;
        }
        return false;
    }

    public boolean increment(int row, int col) {
        checkIndices(row, col);
        
        if (!matrix[row][col]) {
        	columnCounts.increment(col);
            matrix[row][col] = true;
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

		if (inactiveCols.size() == 0) {
			doubleSize();
		}
		Integer kInteger = inactiveCols.removeFirst();
		int k = kInteger.intValue();
		increment(row, k);
		return k;
	}
	
    private void doubleSize() {
        curMaxCol *= 2;
        for (int i=0; i<numRows; i++) {
            matrix[i] = Utilities.copyOf(matrix[i], curMaxCol);
        }
        for (int i=curMaxCol/2; i<curMaxCol; i++) {
        	inactiveCols.add(i);
        }
    }
    
    private void checkIndices(int row, int col) {
    	checkRow(row);
    	checkColumn(col);
    }

	private void checkColumn(int col) {
		if (col >= curMaxCol) {
    		throw new IllegalArgumentException("Cannot change an inactive column: " + col);
        }
	}

	private void checkRow(int row) {
		if (row >= numRows) {
    		throw new IllegalArgumentException("Row exceeds numRows: " + row);	
    	}
	}

    
}
