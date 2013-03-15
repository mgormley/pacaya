/**
 * 
 */
package edu.jhu.hltcoe.lp;

import edu.jhu.hltcoe.util.SafeCast;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;

import java.util.ArrayList;
import java.util.Arrays;

import edu.jhu.hltcoe.util.vector.SortedLongDoubleVector;

/**
 * Represents a set of updates to linear programming matrix rows (e.g. the
 * matrix A in Ax <= b).
 * 
 * These updates are stored as the new row and an index for that row
 * corresponding to the row that should be updated.
 * 
 * @author mgormley
 * 
 */
public class LpMatrixUpdates {
    private TIntArrayList rowIdxs;
    private ArrayList<SortedLongDoubleVector> coefs;

    public LpMatrixUpdates() {
        rowIdxs = new TIntArrayList();
        coefs = new ArrayList<SortedLongDoubleVector>();
    }

    public void add(int rowIdx, SortedLongDoubleVector coef) {
        rowIdxs.add(rowIdx);
        coefs.add(coef);
    }

    public void updateRowsInMatrix(IloLPMatrix mat) throws IloException {
        TIntArrayList rowInd = new TIntArrayList();
        TIntArrayList colInd = new TIntArrayList();
        TDoubleArrayList val = new TDoubleArrayList();
        for (int i = 0; i < rowIdxs.size(); i++) {
            int rowind = rowIdxs.get(i);
            SortedLongDoubleVector row = coefs.get(i);
            rowInd.add(getRowIndArray(row, rowind));
            colInd.add(SafeCast.safeLongToInt(row.getIndices()));
            val.add(row.getValues());
        }
        mat.setNZs(rowInd.toNativeArray(), colInd.toNativeArray(), val.toNativeArray());
    }

    public ArrayList<SortedLongDoubleVector> getAllCoefs() {
        return coefs;
    }

    public void setAllCoefs(ArrayList<SortedLongDoubleVector> coefs) {
        this.coefs = coefs;
    }

    /**
     * Gets an int array of the same length as row.getIndex() and filled
     * with rowind.
     */
    private static int[] getRowIndArray(SortedLongDoubleVector row, int rowind) {
        int[] array = new int[row.getIndices().length];
        Arrays.fill(array, rowind);
        return array;
    }
}