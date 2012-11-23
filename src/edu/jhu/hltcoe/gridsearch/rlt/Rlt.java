package edu.jhu.hltcoe.gridsearch.rlt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.List;

import no.uib.cipr.matrix.sparse.FastSparseVector;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Utilities;

public class Rlt {

    private static final Logger log = Logger.getLogger(Rlt.class);
    
    /**
     * The CPLEX representation of positive infinity.
     */
    public static final double CPLEX_POS_INF = Double.MAX_VALUE;
    /**
     * The CPLEX representation of negative infinity.
     */
    public static final double CPLEX_NEG_INF = -Double.MAX_VALUE;
    
    public static class RltProgram {

        private IloLPMatrix lpMat;
        private IloNumVar[][] rltVars;

        public RltProgram(IloLPMatrix lpMat, IloNumVar[][] rltVars) {
            this.lpMat = lpMat;
            this.rltVars = rltVars;
        }

        public IloLPMatrix getLPMatrix() {
            return lpMat;
        }

        public IloNumVar[][] getRltVars() {
            return rltVars;
        }

    }

    private static class Factor {
        double g;
        FastSparseVector G;

        public Factor(int Ncols, double g, int[] Gind, double[] Gval) {
            this.g = g;
            this.G = new FastSparseVector(Ncols, Gind, Gval);
        }
        
        @Override
        public String toString() {
            return String.format("g=%f G=%s", g, G.toString());
        }
    }

    public static RltProgram getFirstOrderRlt(IloCplex cplex, IloLPMatrix mat) throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();

        // The size of the vectors. This is over-generous but it doesn't make
        // any difference b/c we use sparse vectors.
        int size = n + m*m; 

        List<Factor> factors = getFactors(mat, n, m, numVars, size);

        return getRltConstraints(cplex, n, numVars, size, factors);
    }

    /**
     * Creates the constraint and bounds factors.
     */
    private static List<Factor> getFactors(IloLPMatrix mat, int n, int m, IloNumVar[] numVars, int size)
            throws IloException {
        List<Factor> factors = new ArrayList<Factor>();

        // Add constraint factors.
        double[] lb = new double[m];
        double[] ub = new double[m];
        int[][] Aind = new int[m][];
        double[][] Aval = new double[m][];
        mat.getRows(0, m, lb, ub, Aind, Aval);
        for (int i = 0; i < m; i++) {
            if (lb[i] != CPLEX_NEG_INF) {
                // b <= A_i x
                // 0 <= A_i x - b = (-b - (-A_i x))
                double[] vals = Utilities.copyOf(Aval[i]);
                Vectors.scale(vals, -1.0);
                factors.add(new Factor(size, -lb[i], Aind[i], vals));
            }
            if (ub[i] != CPLEX_POS_INF) {
                // A_i x <= b
                // 0 <= b - A_i x
                factors.add(new Factor(size, ub[i], Aind[i], Aval[i]));
            }
            // TODO: special handling of equality constraints.
        }

        // Add bounds factors.
        for (int i = 0; i < n; i++) {
            double varLb = numVars[i].getLB();
            double varUb = numVars[i].getUB();
            int[] varInd = new int[] { i };
            if (varLb != CPLEX_NEG_INF) {
                // varLb <= x_i
                // 0 <= x_i - varLb = -varLb - (-x_i)
                double[] varVal = new double[] { -1.0 };
                factors.add(new Factor(size, -varLb, varInd, varVal));
            }
            if (varUb != CPLEX_POS_INF) {
                // x_i <= varUb
                // 0 <= varUb - x_i
                double[] varVal = new double[] { 1.0 };
                factors.add(new Factor(size, varUb, varInd, varVal));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("factors: ");
            for (Factor f : factors) {
                log.debug("\t" + f);
            }
        }
        return factors;
    }

    private static RltProgram getRltConstraints(IloCplex cplex, int n, IloNumVar[] numVars, int size,
            List<Factor> factors) throws IloException {
        // Create the first-order RLT variables.
        IloNumVar[][] rltVars = new IloNumVar[n][];
        for (int i = 0; i < n; i++) {
            rltVars[i] = new IloNumVar[i + 1];
            for (int j = 0; j <= i; j++) {
                rltVars[i][j] = cplex.numVar(numVars[i].getLB() * numVars[j].getLB(), numVars[i].getUB()
                        * numVars[j].getUB(), String.format("w_{%d,%d}", i, j));
            }
        }
        
        // Reformulate and linearize the constraints.

        // Add the columns to the matrix.
        IloLPMatrix rltMat = cplex.LPMatrix();
        rltMat.addCols(numVars);
        for (int i = 0; i < n; i++) {
            rltMat.addCols(rltVars[i]);
        }

        // Get rltVar column indices.
        int[][] rltVarsInd = new int[n][];
        for (int i = 0; i < n; i++) {
            rltVarsInd[i] = new int[i + 1];
            for (int j = 0; j <= i; j++) {
                rltVarsInd[i][j] = rltMat.getIndex(rltVars[i][j]);
            }
        }

        // Build the RLT constraints.
        for (int i = 0; i < factors.size(); i++) {
            Factor facI = factors.get(i);
            for (int j = 0; j < factors.size(); j++) {
                Factor facJ = factors.get(j);
                // Here we add the following constraint:
                // \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
                // + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
                // + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk}) w_{kl} &\leq g_ig_j

                SparseVector row = new FastSparseVector(size);
                // Part 1: \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
                SparseVector facIG = facI.G.copy();
                SparseVector facJG = facJ.G.copy();
                row.add(facIG.scale(facJ.g));
                row.add(facJG.scale(facI.g));

                // Part 2: + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
                SparseVector ip = facI.G.hadamardProd(facJ.G);
                ip = ip.scale(-1.0);
                SparseVector shiftedIp = new SparseVector(size);
                for (int idx = 0; idx < ip.getUsed(); idx++) {
                    int k = ip.getIndex()[idx];
                    double val = ip.getData()[idx];
                    shiftedIp.set(rltVarsInd[k][k], val);
                }
                row = (SparseVector) row.add(shiftedIp);

                // Part 3: + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk}) w_{kl}
                for (int ii = 0; ii < facI.G.getUsed(); ii++) {
                    int k = facI.G.getIndex()[ii];
                    double vi = facI.G.getData()[ii];
                    for (int jj = 0; jj < facJ.G.getUsed(); jj++) {
                        int l = facJ.G.getIndex()[jj];
                        double vj = facJ.G.getData()[jj];
                        if (k == l) {
                            continue;
                        }
                        row.add(rltVarsInd[Math.max(k, l)][Math.min(k, l)], -vi * vj);
                    }
                }
                
                // Add the complete constraint.
                
                String name = String.format("cons_{%d, %d}", i, j);
                int rowind = rltMat.addRow(CPLEX_NEG_INF, facI.g * facJ.g, row.getIndex(), row.getData());
                rltMat.getRange(rowind).setName(name);
                
                log.debug(name + " " + row + " <= " + facI.g * facJ.g);
            }
        }

        return new RltProgram(rltMat, rltVars);
    }

}
