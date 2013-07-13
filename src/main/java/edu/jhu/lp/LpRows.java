/**
 * 
 */
package edu.jhu.lp;

import edu.jhu.prim.list.DoubleArrayList;
import edu.jhu.prim.vector.LongDoubleSortedVector;
import edu.jhu.util.SafeCast;
import edu.jhu.util.cplex.CplexUtils;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloRange;

import java.util.ArrayList;


/**
 * Represents a set of linear programming constraints of the form d <= Ax <= b.
 * This representation allows names to be given for each constraint.
 * 
 * @author mgormley
 * 
 */
public class LpRows {
    private DoubleArrayList lbs;
    private DoubleArrayList ubs;
    private ArrayList<LongDoubleSortedVector> coefs;
    private ArrayList<String> names;
    private boolean setNames;

    public LpRows(boolean setNames) {
        lbs = new DoubleArrayList();
        coefs = new ArrayList<LongDoubleSortedVector>();
        ubs = new DoubleArrayList();
        names = new ArrayList<String>();
        this.setNames = setNames;
    }

    public int addRow(double lb, LongDoubleSortedVector coef, double ub) {
        return addRow(lb, coef, ub, null);
    }

    public void addRow(LpRow row) {
        addRow(row.getLb(), row.getCoefs(), row.getUb(), row.getName());
    }

    public int addRow(double lb, LongDoubleSortedVector coef, double ub, String name) {
        lbs.add(lb);
        coefs.add(coef);
        ubs.add(ub);
        names.add(name);
        return lbs.size() - 1;
    }

    /**
     * Construct and return the ith row.
     */
    public LpRow get(int i) {
        return new LpRow(lbs.get(i), coefs.get(i), ubs.get(i), names.get(i));
    }

    /**
     * Adds the stored rows to the matrix.
     * 
     * @return The index of the first newly added row.
     */
    public int addRowsToMatrix(IloLPMatrix mat) throws IloException {
        int[][] ind = new int[coefs.size()][];
        double[][] val = new double[coefs.size()][];
        for (int i = 0; i < coefs.size(); i++) {
            ind[i] = SafeCast.safeLongToInt(coefs.get(i).getIndices());
            val[i] = coefs.get(i).getValues();
        }
        double[] safeLbs = CplexUtils.safeGetBounds(lbs.toNativeArray());
        double[] safeUbs = CplexUtils.safeGetBounds(ubs.toNativeArray());
        int startRow = mat.addRows(safeLbs, safeUbs, ind, val);
        if (setNames) {
            IloRange[] ranges = mat.getRanges();
            for (int i = 1; i <= names.size(); i++) {
                String name = names.get(names.size() - i);
                if (name != null) {
                    ranges[ranges.length - i].setName(name);
                }
            }
        }
        return startRow;
    }

    public int getNumRows() {
        return lbs.size();
    }

    public ArrayList<LongDoubleSortedVector> getAllCoefs() {
        return coefs;
    }

    public void setAllCoefs(ArrayList<LongDoubleSortedVector> coefs) {
        this.coefs = coefs;
    }

}