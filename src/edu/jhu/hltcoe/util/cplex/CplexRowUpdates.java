/**
 * 
 */
package edu.jhu.hltcoe.util.cplex;

import edu.jhu.hltcoe.util.SafeCast;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;

import java.util.ArrayList;
import java.util.Arrays;

import no.uib.cipr.matrix.sparse.longs.SparseLVector;

public class CplexRowUpdates {
    private TIntArrayList rowIdxs;
    private ArrayList<SparseLVector> coefs;

    public CplexRowUpdates() {
        rowIdxs = new TIntArrayList();
        coefs = new ArrayList<SparseLVector>();
    }

    public void add(int rowIdx, SparseLVector coef) {
        rowIdxs.add(rowIdx);
        coefs.add(coef);
    }

    public void updateRowsInMatrix(IloLPMatrix mat) throws IloException {
        TIntArrayList rowInd = new TIntArrayList();
        TIntArrayList colInd = new TIntArrayList();
        TDoubleArrayList val = new TDoubleArrayList();
        for (int i = 0; i < rowIdxs.size(); i++) {
            int rowind = rowIdxs.get(i);
            SparseLVector row = coefs.get(i);
            rowInd.add(getRowIndArray(row, rowind));
            colInd.add(SafeCast.safeToInt(row.getIndex()));
            val.add(row.getData());
        }
        mat.setNZs(rowInd.toNativeArray(), colInd.toNativeArray(), val.toNativeArray());
    }

    public ArrayList<SparseLVector> getAllCoefs() {
        return coefs;
    }

    public void setAllCoefs(ArrayList<SparseLVector> coefs) {
        this.coefs = coefs;
    }

    /**
     * Gets an int array of the same length as row.getIndex() and filled
     * with rowind.
     */
    private static int[] getRowIndArray(SparseLVector row, int rowind) {
        int[] array = new int[row.getIndex().length];
        Arrays.fill(array, rowind);
        return array;
    }
}