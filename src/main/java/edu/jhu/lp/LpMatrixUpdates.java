/**
 * 
 */
package edu.jhu.lp;

import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.prim.vector.LongDoubleSortedVector;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;

import java.util.ArrayList;
import java.util.Arrays;


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
    private IntArrayList rowIdxs;
    private ArrayList<LongDoubleSortedVector> coefs;

    public LpMatrixUpdates() {
        rowIdxs = new IntArrayList();
        coefs = new ArrayList<LongDoubleSortedVector>();
    }

    public void add(int rowIdx, LongDoubleSortedVector coef) {
        rowIdxs.add(rowIdx);
        coefs.add(coef);
    }

    public void updateRowsInMatrix(IloLPMatrix mat) throws IloException {
        IntArrayList rowInd = new IntArrayList();
        IntArrayList colInd = new IntArrayList();
        DoubleArrayList val = new DoubleArrayList();
        for (int i = 0; i < rowIdxs.size(); i++) {
            int rowind = rowIdxs.get(i);
            LongDoubleSortedVector row = coefs.get(i);
            rowInd.add(getRowIndArray(row, rowind));
            colInd.add(SafeCast.safeLongToInt(row.getIndices()));
            val.add(row.getValues());
        }
        mat.setNZs(rowInd.toNativeArray(), colInd.toNativeArray(), val.toNativeArray());
    }

    public ArrayList<LongDoubleSortedVector> getAllCoefs() {
        return coefs;
    }

    public void setAllCoefs(ArrayList<LongDoubleSortedVector> coefs) {
        this.coefs = coefs;
    }

    /**
     * Gets an int array of the same length as row.getIndex() and filled
     * with rowind.
     */
    private static int[] getRowIndArray(LongDoubleSortedVector row, int rowind) {
        int[] array = new int[row.getIndices().length];
        Arrays.fill(array, rowind);
        return array;
    }
}