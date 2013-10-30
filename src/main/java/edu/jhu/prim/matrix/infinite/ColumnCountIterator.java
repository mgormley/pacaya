/**
 * 
 */
package edu.jhu.prim.matrix.infinite;

import java.util.Iterator;

public class ColumnCountIterator implements Iterator<Integer>, Iterable<Integer> {
    
    private int[] columnCounts;
    private int i = 0;
    
    public ColumnCountIterator(int[] columnCounts) {
        this.columnCounts = columnCounts;
        moveToNextActiveIndex();
    }

    public boolean hasNext() {
        if (i < columnCounts.length) {
            return true;
        }
        return false;
    }

    public Integer next() {
        int ret = i++;
        moveToNextActiveIndex();
        return ret;
    }

    private void moveToNextActiveIndex() {
        while (i < columnCounts.length && columnCounts[i] == 0) { 
            i++;
        }
    }

    public void remove() {
        throw new RuntimeException("not implemented");
    }

    public Iterator<Integer> iterator() {
        return this;
    }
    
}