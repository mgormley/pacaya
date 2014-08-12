package edu.jhu.data;

import java.io.Serializable;
import java.util.Arrays;

import edu.jhu.prim.list.IntStack;
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

    public void and(DepEdgeMask other) {
        this.mat.and(other.mat);
    }

    public boolean allowsSinglyRootedTrees() {
        for (int c=0; c < n; c++) {
            // For each child of the root, check that a singly root tree 
            if(this.isKept(-1, c) && allowsSinglyRootedTrees(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean allowsSinglyRootedTrees(int root) {
        boolean[] marked = new boolean[n];
        Arrays.fill(marked, false);
        // Run a depth-first search (excluding edges to the root) starting at the given token, and mark all reachable tokens.
        IntStack stack = new IntStack();
        stack.push(root);
        int numMarked = 0;
        while (stack.size() > 0) {
            int p = stack.pop();
            if (marked[p]) {
                continue;
            }
            for (int c=0; c<n; c++) {
                if (p != c && !marked[c] && this.isKept(p, c)) {
                    stack.push(c);
                }
            }
            marked[p] = true;
            numMarked++;
        }
        // If all tokens are reachable, then a singly root tree is possible.
        return numMarked == n;
    }
    
}
