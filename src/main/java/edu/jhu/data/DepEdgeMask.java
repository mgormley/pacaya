package edu.jhu.data;

import java.io.Serializable;

import edu.jhu.prim.matrix.BitSetBinaryMatrix;

public class DepEdgeMask implements Serializable {

    private static final long serialVersionUID = 1L;

    private int n;
    private BitSetBinaryMatrix mat;
    
    public DepEdgeMask(DepEdgeMask mask) {
        this.mat = new BitSetBinaryMatrix(mask.mat);
        this.n = mask.n;
    }

    public DepEdgeMask(int numWords, boolean isKept) {
        this.n = numWords;
        this.mat = new BitSetBinaryMatrix(n+1, n+1);
        setIsKeptAll(isKept);
    }

    /** Returns whether the corresponding dependency arc should be pruned. */
    public boolean isPruned(int parent, int child) {
        return !isKept(parent, child);
    }

    /** Returns whether the corresponding dependency arc should be pruned. */
    public boolean isKept(int parent, int child) {
        checkIndices(parent, child);
        return mat.get(parent+1, child+1);
    }
    
    private void checkIndices(int parent, int child) {
        if (!(-1 <= parent && parent < n && 0 <= child && child < n && child != parent)) {
            throw new IllegalArgumentException("Invalid parent/child indices: " + parent + " " + child);
        }
    }
    
    public void setIsKept(int parent, int child, boolean isKept) {
        checkIndices(parent, child);
        if (isKept) {
            mat.increment(parent+1, child+1);
        } else {
            mat.decrement(parent+1, child+1);
        }
    }
    
    public void setIsPruned(int parent, int child, boolean isPruned) {
        setIsKept(parent, child, !isPruned);
    }

    /** Gets the number of kept parents for the given child. */
    public int getParentCount(int child) {
        checkIndices(-1, child);
        return mat.getColumnCount(child+1);
    }
    
    public void setIsKeptAll(boolean isKept) {
        for (int p=-1; p<n; p++) {
            for(int c=0; c<n; c++) {
                if (p == c) { continue; }
                setIsKept(p, c, isKept);
            }
        }
    }

    /** Gets the total number of kept edges. */
    public int getCount() {
        return mat.getCount();
    }

    @Override
    public String toString() {
        return "DepEdgeMask [n=" + n + ", mat=\n" + mat + "]";
    }        
    
}
