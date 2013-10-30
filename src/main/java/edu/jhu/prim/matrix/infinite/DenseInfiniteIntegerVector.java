package edu.jhu.prim.matrix.infinite;

import java.util.Arrays;

import edu.jhu.util.Utilities;

public class DenseInfiniteIntegerVector implements InfiniteIntegerVector {

    private int[] columnCounts;
    private int curMaxCol;

    /**
     * An infinite number of columns is assumed.
     */
    public DenseInfiniteIntegerVector() {
        this(1);
    }

    public DenseInfiniteIntegerVector(int initialNumberOfColumns) {
        curMaxCol = initialNumberOfColumns;
        columnCounts = new int[curMaxCol];
        Arrays.fill(columnCounts, 0);
    }

    public int get(int k) {
        if (k >= curMaxCol) {
            return 0;
        }
        return columnCounts[k];
    }
    
    public void decrement(int k) {
        decrement(k, 1);
    }

    public void increment(int k) {
    	increment(k, 1);
    }

    private void checkColumnForMod(int k) {
        while (k >= curMaxCol) {
            doubleSize();
        }
    }

    private void doubleSize() {
        curMaxCol *= 2;
        columnCounts = Utilities.copyOf(columnCounts, curMaxCol);
    }

    public Iterable<Integer> getActiveIndices() {
        return new ColumnCountIterator(columnCounts);
    }

	public void decrement(int k, int decr) {
		checkColumnForMod(k);
        columnCounts[k] -= decr;
	}

	public void increment(int k, int incr) {
		checkColumnForMod(k);
        columnCounts[k] += incr;
	}

}
