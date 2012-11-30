package edu.jhu.hltcoe.gridsearch.rlt;

import ilog.concert.IloNumVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SymmetricMatrix<T> {

    /**
     * Convenience class for Integers.
     */
    public static class SymIntMat extends SymmetricMatrix<Integer> { 
        public Integer[] getRowAsArray(int i) {
            return getRowAsArray(i, new Integer[]{});
        }

        public void incrementAll(int incr) {
            for (int i=0; i<matrix.size(); i++) {
                ArrayList<Integer> row = matrix.get(i);
                if (row != null) {
                    for (int j=0; j<row.size(); j++) {
                        Integer intObj = row.get(j);
                        if (intObj != null) {
                            row.set(j, intObj + incr);
                        }
                    }
                }
            }
        }

        public void setAll(SymIntMat other) {
            for (int i=0; i<other.matrix.size(); i++) {
                ArrayList<Integer> row = other.matrix.get(i);
                if (row != null) {
                    for (int j=0; j<row.size(); j++) {
                        Integer intObj = row.get(j);
                        if (intObj != null) {
                            this.set(i, j, intObj);
                        }
                    }
                }
            }
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

    protected ArrayList<ArrayList<T>> matrix;
    
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
        return i < matrix.size() && j < matrix.get(i).size() && matrix.get(i).get(j) != null;
    }
    
    public T get(int i, int j) {
        return matrix.get(Math.max(i, j)).get(Math.min(i,j));
    }

    public int getNrows() {
        return matrix.size();
    }
    
    public int getNumNonNull() {
        int numNonNull = 0;
        for (ArrayList<T> row : matrix) {
            for (T val : row) {
                if (val != null) {
                    numNonNull++;
                }
            }
        }
        return numNonNull;
    }

    public T[] getRowAsArray(int i, T[] row) {
        return matrix.get(i).toArray(row);
    }
    
    public List<T> getRowAsList(int i) {
        return Collections.unmodifiableList(matrix.get(i));
    }

}
