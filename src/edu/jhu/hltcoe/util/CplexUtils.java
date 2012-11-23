package edu.jhu.hltcoe.util;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class CplexUtils {

    /**
     * Helper method for getting a 2D array of CPLEX variables.
     * @throws IloException 
     */
    public static double[][] getValues(IloCplex cplex, IloNumVar[][] vars) throws IloException {
        double[][] vals = new double[vars.length][];
        for (int i=0; i<vars.length; i++) {
            vals[i] = cplex.getValues(vars[i]);
        }
        return vals;
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

}
