package edu.jhu.gridsearch.rlt;

import ilog.concert.IloNumVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.jhu.prim.tuple.ComparablePair;

public class SymmetricMatrix<T> {

    protected class DMatPair extends ComparablePair<Integer, Integer> {
        public DMatPair(Integer x, Integer y) {
            super(x, y);
        }
    }

    protected class DMat extends HashMap<DMatPair, T> {
        private static final long serialVersionUID = -6515440327553417574L;
    }

    /**
     * Convenience class for Doubles.
     */
    public static class SymDoubleMat extends SymmetricMatrix<Double> {
        public Double[] getRowAsArray(int i) {
            return getRowAsArray(i, new Double[] {});
        }
    }

    /**
     * Convenience class for IloNumVars.
     */
    public static class SymVarMat extends SymmetricMatrix<IloNumVar> {
        public IloNumVar[] getRowAsArray(int i) {
            return getRowAsArray(i, new IloNumVar[] {});
        }
    }

    protected DMat matrix;

    public SymmetricMatrix() {
        matrix = new DMat();
    }

    public void set(int iIdx, int jIdx, T value) {
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        matrix.put(new DMatPair(i, j), value);
    }

    public boolean contains(int iIdx, int jIdx) {
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        return matrix.containsKey(new DMatPair(i, j));
    }

    public T get(int iIdx, int jIdx) {
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        return matrix.get(new DMatPair(i, j));
    }

    public int getNrows() {
        int maxRowIdx = -1;
        for (Entry<DMatPair, T> e : matrix.entrySet()) {
            if (e.getKey().get1() > maxRowIdx) {
                maxRowIdx = e.getKey().get1();
            }
        }
        return maxRowIdx + 1;
    }

    public T[] getRowAsArray(int i, T[] row) {
        return getRowAsList(i).toArray(row);
    }

    public List<T> getRowAsList(int i) {
        List<T> row = new ArrayList<T>();
        Object[] keys = matrix.keySet().toArray();
        Arrays.sort(keys);
        for (Object key : keys) {
            if (((DMatPair)key).get1() == i) {
                row.add(matrix.get(key));
            }
        }
        return Collections.unmodifiableList(row);
    }

}
