/**
 * 
 */
package edu.jhu.hltcoe.util.cplex;

import edu.jhu.hltcoe.util.SafeCast;
import gnu.trove.TDoubleArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloRange;

import java.util.ArrayList;

import no.uib.cipr.matrix.sparse.longs.SparseLVector;

public class CplexRows {
    private TDoubleArrayList lbs;
    private TDoubleArrayList ubs;
    private ArrayList<SparseLVector> coefs;
    private ArrayList<String> names;
    private boolean setNames;

    public CplexRows(boolean setNames) {
        lbs = new TDoubleArrayList();
        coefs = new ArrayList<SparseLVector>();
        ubs = new TDoubleArrayList();
        names = new ArrayList<String>();
        this.setNames = setNames;
    }

    public int addRow(double lb, SparseLVector coef, double ub) {
        return addRow(lb, coef, ub, null);
    }

    public void addRow(CplexRow row) {
        addRow(row.getLb(), row.getCoefs(), row.getUb(), row.getName());
    }

    public int addRow(double lb, SparseLVector coef, double ub, String name) {
        lbs.add(lb);
        coefs.add(coef);
        ubs.add(ub);
        names.add(name);
        return lbs.size() - 1;
    }

    /**
     * Construct and return the ith row.
     */
    public CplexRow get(int i) {
        return new CplexRow(lbs.get(i), coefs.get(i), ubs.get(i), names.get(i));
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
            ind[i] = SafeCast.safeToInt(coefs.get(i).getIndex());
            val[i] = coefs.get(i).getData();
        }
        int startRow = mat.addRows(lbs.toNativeArray(), ubs.toNativeArray(), ind, val);
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

    public ArrayList<SparseLVector> getAllCoefs() {
        return coefs;
    }

    public void setAllCoefs(ArrayList<SparseLVector> coefs) {
        this.coefs = coefs;
    }

}