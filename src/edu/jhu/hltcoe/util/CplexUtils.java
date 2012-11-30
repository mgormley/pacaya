package edu.jhu.hltcoe.util;

import static org.junit.Assert.assertTrue;
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;

import no.uib.cipr.matrix.sparse.FastSparseVector;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.junit.Assert;

public class CplexUtils {

    public static class CplexRowUpdates {
        private TIntArrayList rowIdxs;
        private ArrayList<SparseVector> coefs;
 
        public CplexRowUpdates() {
            rowIdxs = new TIntArrayList();
            coefs = new ArrayList<SparseVector>();
        }

        public void add(int rowIdx, SparseVector coef) {
            rowIdxs.add(rowIdx);
            coefs.add(coef);
        }

        public void updateRowsInMatrix(IloLPMatrix mat) throws IloException {
            for (int i=0; i<rowIdxs.size(); i++) {
                int rowind = rowIdxs.get(i);
                SparseVector row = coefs.get(i);
                mat.setNZs(getRowIndArray(row, rowind), row.getIndex(), row.getData());
            }
        }

        public ArrayList<SparseVector> getAllCoefs() {
            return coefs;
        }

        public void setAllCoefs(ArrayList<SparseVector> coefs) {
            this.coefs = coefs;
        }

        /**
         * Gets an int array of the same length as row.getIndex() and filled with
         * rowind.
         */
        private static int[] getRowIndArray(SparseVector row, int rowind) {
            int[] array = new int[row.getIndex().length];
            Arrays.fill(array, rowind);
            return array;
        }
    }
    
    public static class CplexRow {
        private double lb;
        private double ub;
        private SparseVector coefs;
        private String name;
        public CplexRow(double lb, SparseVector coefs, double ub, String name) {
            super();
            this.lb = lb;
            this.coefs = coefs;
            this.ub = ub;
            this.name = name;
        }
        public double getLb() {
            return lb;
        }
        public double getUb() {
            return ub;
        }
        public SparseVector getCoefs() {
            return coefs;
        }
        public String getName() {
            return name;
        }
    }
    
    public static class CplexRows {
        private TDoubleArrayList lbs;
        private TDoubleArrayList ubs;
        private ArrayList<SparseVector> coefs;
        private ArrayList<String> names;
        private boolean setNames;

        public CplexRows(boolean setNames) {
            lbs = new TDoubleArrayList();
            coefs = new ArrayList<SparseVector>();
            ubs = new TDoubleArrayList();
            names = new ArrayList<String>();
            this.setNames = setNames;
        }
        
        public int addRow(double lb, SparseVector coef, double ub) {
            return addRow(lb, coef, ub, null);
        }

        public void addRow(CplexRow row) {
            addRow(row.getLb(), row.getCoefs(), row.getUb(), row.getName());
        }
        
        public int addRow(double lb, SparseVector coef, double ub, String name) {
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
         * @return The index of the first newly added row.
         */
        public int addRowsToMatrix(IloLPMatrix mat) throws IloException {
            int[][] ind = new int[coefs.size()][];
            double[][] val = new double[coefs.size()][];
            for (int i=0; i<coefs.size(); i++) {
                ind[i] = coefs.get(i).getIndex();
                val[i] = coefs.get(i).getData();
            }
            int startRow = mat.addRows(lbs.toNativeArray(), ubs.toNativeArray(), ind, val);
            if (setNames) {
                IloRange[] ranges = mat.getRanges();
                for (int i=1; i<=names.size(); i++) {
                    String name = names.get(names.size() - i);
                    if (name != null) {
                        ranges[ranges.length-i].setName(name);
                    }
                }
            }
            return startRow;
        }

        public int getNumRows() {
            return lbs.size();
        }

        public ArrayList<SparseVector> getAllCoefs() {
            return coefs;
        }

        public void setAllCoefs(ArrayList<SparseVector> coefs) {
            this.coefs = coefs;
        }
        
    }
    
    /**
     * Helper method for getting a 3D array of CPLEX variables.
     * @throws IloException 
     */
    public static double[][][] getValues(IloCplex cplex, IloNumVar[][][] vars) throws IloException {
        double[][][] vals = new double[vars.length][][];
        for (int i=0; i<vars.length; i++) {
            vals[i] = getValues(cplex, vars[i]);
        }
        return vals;
    }
    
    /**
     * Helper method for getting a 2D array of CPLEX variables.
     * @throws IloException 
     */
    public static double[][] getValues(IloCplex cplex, IloNumVar[][] vars) throws IloException {
        double[][] vals = new double[vars.length][];
        for (int i=0; i<vars.length; i++) {
            vals[i] = getValues(cplex, vars[i]);
        }
        return vals;
    }

    /**
     * Helper method for getting a 2D array of CPLEX variables.
     * @throws IloException 
     */
    public static double[][] getValues(IloCplex cplex, SymVarMat vars) throws IloException {
        double[][] vals = new double[vars.getNrows()][];
        for (int i=0; i<vars.getNrows(); i++) {
            IloNumVar[] varsI = vars.getRowAsArray(i);
            vals[i] = getValues(cplex, varsI);
        }
        return vals;
    }
        
    /**
     * Helper method for getting a 1D array of CPLEX variables.
     * @throws IloException 
     */
    public static double[] getValues(IloCplex cplex, IloNumVar[] vars) throws IloException {
        double[] vals = new double[vars.length];
        for (int i=0; i<vars.length; i++) {
            if (vars[i] != null) {
                vals[i] = cplex.getValue(vars[i]);
            } else {
                vals[i] = 0.0; //TODO: Double.NaN;
            }
        }
        return vals;
    }

    public static void addRows(IloLPMatrix mat, IloRange[][][] ranges) throws IloException {
        for (int i = 0; i < ranges.length; i++) {
            addRows(mat, ranges[i]);
        }
    }

    public static void addRows(IloLPMatrix mat, IloRange[][] ranges) throws IloException {
        for (int i = 0; i < ranges.length; i++) {
            addRows(mat, ranges[i]);
        }
    }
    
    public static void addRows(IloLPMatrix mat, IloRange[] ranges) throws IloException {
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i] != null) {
                mat.addRow(ranges[i]);
            }
        }
    }

    // -------- JUnit Assertions -----------
    
    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow) throws IloException {
        int nCols = rltMat.getNcols();
        assertTrue(nCols == denseRow.length);
        int nRows = rltMat.getNrows();
        double[] lbs = new double[nRows];
        double[] ubs = new double[nRows];
        int[][] ind = new int[nRows][];
        double[][] val = new double[nRows][];
        rltMat.getRows(0, nRows, lbs, ubs, ind, val);
        
        FastSparseVector expectedRow = new FastSparseVector(denseRow);
        
        for (int m=0; m<nRows; m++) {
            FastSparseVector row = new FastSparseVector(ind[m], val[m]);
            //System.out.println(row + "\n" + expectedRow + "\n" + row.equals(expectedRow, 1e-13));
            if (row.equals(expectedRow, 1e-13)) {
                return;
            }
        }
        Assert.fail("Matrix does not contain row: " + Arrays.toString(denseRow));
    }
    
    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow, double lb, double ub) throws IloException {
        int nCols = rltMat.getNcols();
        assertTrue(nCols == denseRow.length);
        int nRows = rltMat.getNrows();
        double[] lbs = new double[nRows];
        double[] ubs = new double[nRows];
        int[][] ind = new int[nRows][];
        double[][] val = new double[nRows][];
        rltMat.getRows(0, nRows, lbs, ubs, ind, val);
        
        FastSparseVector expectedRow = new FastSparseVector(denseRow);
        
        for (int m=0; m<nRows; m++) {
            FastSparseVector row = new FastSparseVector(ind[m], val[m]);
            //System.out.println(row + "\n" + expectedRow + "\n" + row.equals(expectedRow, 1e-13));
            if (row.equals(expectedRow, 1e-13) && Utilities.equals(lb, lbs[m], 1e-13)
                    && Utilities.equals(ub, ubs[m], 1e-13)) {
                return;
            }
        }
        Assert.fail("Matrix does not contain row: " + Arrays.toString(denseRow));
    }

}
