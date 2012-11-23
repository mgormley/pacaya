package edu.jhu.hltcoe.util;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

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

}
