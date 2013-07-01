package edu.jhu.util.matrix.infinite;

import java.util.BitSet;
import java.util.LinkedList;

public class BitSetInfiniteBinaryMatrix implements InfiniteBinaryMatrix {

    private BitSet[] matrix;
    private InfiniteIntegerVector columnCounts;
    private LinkedList<Integer> inactiveCols = new LinkedList<Integer>();

    private final int numRows;
    private int curMaxCol;

    /**
     * An infinite number of columns is assumed.
     */
    public BitSetInfiniteBinaryMatrix(int numRows) {
        this(numRows, 1);
    }

    public BitSetInfiniteBinaryMatrix(int numRows, int initialNumberOfColumns) {
        this.numRows = numRows;
        curMaxCol = initialNumberOfColumns;
        matrix = new BitSet[numRows];
        for (int i = 0; i < numRows; i++) {
            matrix[i] = new BitSet();
        }
        columnCounts = new DenseInfiniteIntegerVector(curMaxCol);
        for (int i = 0; i < curMaxCol; i++) {
            inactiveCols.add(i);
        }
    }

    public int getColumnCount(int k) {
        return columnCounts.get(k);
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
            for (int k = 0; k < bmat[i].length; k++) {
                bmat[i][k] = matrix[i].get(k);
            }
        }
        return bmat;
    }

    public boolean decrement(int row, int k) {
        checkIndices(row, k);

        if (matrix[row].get(k)) {
            columnCounts.decrement(k);
            if (columnCounts.get(k) == 0) {
                Integer kInteger = Integer.valueOf(k);
                inactiveCols.addFirst(kInteger);
            }
            matrix[row].set(k, false);
            return true;
        }
        return false;
    }

    public boolean increment(int row, int k) {
        checkIndices(row, k);

        if (!matrix[row].get(k)) {
            columnCounts.increment(k);
            matrix[row].set(k, true);
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

    private void checkIndices(int row, int k) {
        checkRow(row);
        checkColumn(k);
    }

    private void checkColumn(int k) {
        if (k >= curMaxCol) {
            throw new IllegalArgumentException("Cannot change an inactive column: " + k);
        }
    }

    private void checkRow(int row) {
        if (row >= numRows) {
            throw new IllegalArgumentException("Row exceeds numRows: " + row);
        }
    }

}
