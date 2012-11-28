package edu.jhu.hltcoe.util;

import static org.junit.Assert.assertTrue;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.Arrays;

import no.uib.cipr.matrix.sparse.FastSparseVector;

import org.junit.Assert;

import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymVarMat;

public class CplexUtils {
    
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
