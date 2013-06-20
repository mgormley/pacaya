package edu.jhu.hltcoe.gridsearch.rlt;

import java.util.Map.Entry;

/**
 * Convenience class for Integers.
 */
public class SymIntMat extends SymmetricMatrix<Integer> {  
    
    public Integer[] getRowAsArray(int i) {
        return getRowAsArray(i, new Integer[]{});
    }

    public void incrementAll(int incr) {
        for (Entry<DMatPair, Integer> e : matrix.entrySet()) {
            matrix.put(e.getKey(), e.getValue() + incr);
        }
    }

    public void setAll(SymIntMat other) {
        matrix.putAll(other.matrix);
    }
}