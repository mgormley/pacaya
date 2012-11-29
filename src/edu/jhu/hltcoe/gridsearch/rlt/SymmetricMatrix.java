package edu.jhu.hltcoe.gridsearch.rlt;

import ilog.concert.IloNumVar;

import java.util.ArrayList;

public class SymmetricMatrix<T> {

    /**
     * Convenience class for Integers.
     */
    public static class SymIntMat extends SymmetricMatrix<Integer> { 
        public Integer[] getRowAsArray(int i) {
            return getRowAsArray(i, new Integer[]{});
        }
    }

    /**
     * Convenience class for Doubles.
     */
    public static class SymDoubleMat extends SymmetricMatrix<Double> { 
        public Double[] getRowAsArray(int i) {
            return getRowAsArray(i, new Double[]{});
        }
    }
    
    /**
     * Convenience class for IloNumVars.
     */
    public static class SymVarMat extends SymmetricMatrix<IloNumVar> { 
        public IloNumVar[] getRowAsArray(int i) {
            return getRowAsArray(i, new IloNumVar[]{});
        }
    }

    private ArrayList<ArrayList<T>> matrix;

    public SymmetricMatrix() {
        matrix = new ArrayList<ArrayList<T>>();
    }

    public void set(int iIdx, int jIdx, T value) {
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        expand(i, j);
        matrix.get(i).set(j, value);
    }

    private void expand(int i, int j) {
        while( i >= matrix.size()) {
            matrix.add(new ArrayList<T>());
        }
        ArrayList<T> row = matrix.get(i);
        while (j >= row.size()) {
            row.add(null);
        }
    }
    
    public boolean contains(int iIdx, int jIdx) {
        int i = Math.max(iIdx, jIdx);
        int j = Math.min(iIdx, jIdx);
        return i < matrix.size() && j < matrix.get(i).size();
    }
    
    public T get(int i, int j) {
        return matrix.get(Math.max(i, j)).get(Math.min(i,j));
    }

    public int getNrows() {
        return matrix.size();
    }

    public T[] getRowAsArray(int i, T[] row) {
        return matrix.get(i).toArray(row);
    }

}
