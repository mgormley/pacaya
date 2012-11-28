package edu.jhu.hltcoe.gridsearch.rlt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import no.uib.cipr.matrix.sparse.FastSparseVector;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Pair;
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

        private IloLPMatrix rltMat;
        private IloNumVar[][] rltVars;
        private IloLPMatrix inputMatrix;
        private List<Factor> factors;
        private int constantVarColIdx;
        private int[][] rltVarsInd;
        private int[][] rltConsInd;
        private HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap;

        public RltProgram(IloLPMatrix rltMat, IloNumVar[][] rltVars, IloLPMatrix inputMatrix, List<Factor> factors,
                int constantVarColIdx, int[][] rltVarsInd, int[][] rltConsInd) throws IloException {
            this.rltMat = rltMat;
            this.rltVars = rltVars;
            this.inputMatrix = inputMatrix;
            this.factors = factors;
            this.constantVarColIdx = constantVarColIdx;
            this.rltVarsInd = rltVarsInd;
            this.rltConsInd = rltConsInd;
            
            this.boundsFactorMap = getBoundsFactorMap(rltMat, factors);
        }

        public IloLPMatrix getRltMatrix() {
            return rltMat;
        }

        public IloNumVar[][] getRltVars() {
            return rltVars;
        }

        public IloNumVar getRltVar(IloNumVar var1, IloNumVar var2) throws IloException {
            int idx1 = inputMatrix.getIndex(var1);
            int idx2 = inputMatrix.getIndex(var2);            
            return rltVars[Math.max(idx1, idx2)][Math.min(idx1, idx2)];
        }
        
        public void updateBound(IloNumVar var, Lu lu) throws IloException {
            // Find the factor corresponding to this variable and lower/upper bound.
            int factorIdx = getFactorIdx(var, lu);
            
            // Update the factor.
            BoundFactor bf = (BoundFactor)factors.get(factorIdx);
            Factor factor;
            if (bf.lu == Lu.LOWER){ 
                factor = getBoundFactorLower(inputMatrix.getNumVars(), bf.colIdx);
            } else {
                factor = getBoundFactorUpper(inputMatrix.getNumVars(), bf.colIdx);
            }
            factors.set(factorIdx, factor);
            
            // For each row:
            //  - Update the coefficients.
            //  - Update the upper bound.
            //    This requires adding an auxiliary variable that is fixed to equal 1.0.
            for (int i = 0; i < factors.size(); i++) {
                Factor facI = factors.get(i);
                int rowind = rltConsInd[Math.max(factorIdx, i)][Math.min(factorIdx, i)];
                updateRowNZs(factor, facI, rowind, rltMat, constantVarColIdx, rltVarsInd);
            }
        }

        private int getFactorIdx(IloNumVar var, Lu lu) throws IloException {
            int fIdx = boundsFactorMap.get(new Pair<IloNumVar,Lu>(var, lu));
            BoundFactor bf = (BoundFactor)factors.get(fIdx);
            assert(bf.lu == lu);
            assert(bf.colIdx == rltMat.getIndex(var));
            return fIdx;
        }

        private static HashMap<Pair<IloNumVar, Lu>, Integer> getBoundsFactorMap(IloLPMatrix rltMat, List<Factor> factors) throws IloException {
            HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap = new HashMap<Pair<IloNumVar, Lu>, Integer>();
            for (int i=0; i<factors.size(); i++) {
                Factor f = factors.get(i);
                if (f instanceof BoundFactor) {
                    BoundFactor bf = (BoundFactor)f;
                    boundsFactorMap.put(new Pair<IloNumVar,Lu>(rltMat.getNumVar(bf.colIdx), bf.lu), i);
                }
            }
            return boundsFactorMap;
        }
        
    }

    private static class Factor {
        double g;
        FastSparseVector G;

        public Factor(double g, int[] Gind, double[] Gval) {
            this.g = g;
            this.G = new FastSparseVector(Gind, Gval);
        }
        
        @Override
        public String toString() {
            return String.format("g=%f G=%s", g, G.toString());
        }
    }
    
    private static class RowFactor extends Factor {
        int rowIdx;
        public RowFactor(double g, int[] Gind, double[] Gval, int rowIdx) {
            super(g, Gind, Gval);
            this.rowIdx = rowIdx;
        }
        @Override
        public String toString() {
            return String.format("g=%f G=%s row=%d", g, G.toString(), rowIdx);
        }
    }
    
    private static class BoundFactor extends Factor {
        int colIdx;
        Lu lu;
        public BoundFactor(double g, int[] Gind, double[] Gval, int colIdx, Lu lu) {
            super(g, Gind, Gval);
            this.colIdx = colIdx;
            this.lu = lu;
        }
        @Override
        public String toString() {
            return String.format("g=%f G=%s col=%d %s", g, G.toString(), colIdx, lu);
        }
    }

    private Rlt() {
        // Private constructor.
    }

    public static RltProgram getConvexConcaveEnvelope(IloCplex cplex, IloLPMatrix mat) throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();

        List<Factor> factors = getFactors(mat, n, m, numVars, true);

        return getRltConstraints(cplex, m, n, numVars, factors, mat);
    }

    public static RltProgram getFirstOrderRlt(IloCplex cplex, IloLPMatrix mat) throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();

        List<Factor> factors = getFactors(mat, n, m, numVars, false);

        return getRltConstraints(cplex, m, n, numVars, factors, mat);
    }

    /**
     * Creates the constraint and bounds factors.
     */
    private static List<Factor> getFactors(IloLPMatrix mat, int n, int m, IloNumVar[] numVars, boolean envelopeOnly)
            throws IloException {
        List<Factor> factors = new ArrayList<Factor>();

        if (!envelopeOnly) {
            // Add constraint factors.
            double[] lb = new double[m];
            double[] ub = new double[m];
            int[][] Aind = new int[m][];
            double[][] Aval = new double[m][];
            mat.getRows(0, m, lb, ub, Aind, Aval);
            for (int rowIdx = 0; rowIdx < m; rowIdx++) {
                if (lb[rowIdx] != CPLEX_NEG_INF) {
                    // b <= A_i x
                    // 0 <= A_i x - b = (-b - (-A_i x))
                    double[] vals = Utilities.copyOf(Aval[rowIdx]);
                    Vectors.scale(vals, -1.0);
                    factors.add(new RowFactor(-lb[rowIdx], Aind[rowIdx], vals, rowIdx));
                }
                if (ub[rowIdx] != CPLEX_POS_INF) {
                    // A_i x <= b
                    // 0 <= b - A_i x
                    factors.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx));
                }
                // TODO: special handling of equality constraints.
            }
        }
        
        // Add bounds factors.
        for (int colIdx = 0; colIdx < n; colIdx++) {
            BoundFactor bfLb = getBoundFactorLower(numVars, colIdx);
            if (bfLb != null) {
                factors.add(bfLb);
            }
            BoundFactor bfUb = getBoundFactorUpper(numVars, colIdx);
            if (bfUb != null) {
                factors.add(bfUb);
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

    private static BoundFactor getBoundFactorLower(IloNumVar[] numVars, int colIdx) throws IloException {
        double varLb = numVars[colIdx].getLB();
        if (varLb != CPLEX_NEG_INF) {
            // varLb <= x_i
            // 0 <= x_i - varLb = -varLb - (-x_i)
            int[] varInd = new int[] { colIdx };    
            double[] varVal = new double[] { -1.0 };
            return new BoundFactor(-varLb, varInd, varVal, colIdx, Lu.LOWER);
        }
        return null;
    }
    
    private static BoundFactor getBoundFactorUpper(IloNumVar[] numVars, int colIdx) throws IloException {
        double varUb = numVars[colIdx].getUB();
        if (varUb != CPLEX_POS_INF) {
            // x_i <= varUb
            // 0 <= varUb - x_i
            int[] varInd = new int[] { colIdx };
            double[] varVal = new double[] { 1.0 };
            return new BoundFactor(varUb, varInd, varVal, colIdx, Lu.UPPER);
        }
        return null;
    }

    private static RltProgram getRltConstraints(IloCplex cplex, int m, int n,
            IloNumVar[] numVars, List<Factor> factors, IloLPMatrix inputMatrix) throws IloException {
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

        // Add a variable that is always 1.0. This enables us to update the
        // upper bound without adding/removing rows of the matrix.
        int constantVarColIdx = rltMat.addColumn(cplex.numVar(1, 1, "const"));
        
        // Get rltVar column indices.
        int[][] rltVarsInd = new int[n][];
        for (int i = 0; i < n; i++) {
            rltVarsInd[i] = new int[i + 1];
            for (int j = 0; j <= i; j++) {
                rltVarsInd[i][j] = rltMat.getIndex(rltVars[i][j]);
            }
        }
        
        // Get RLT constraints' row indices.
        int[][] rltConsInd = new int[factors.size()][];
        for (int i = 0; i < factors.size(); i++) {
            rltConsInd[i] = new int[i + 1];
        }

        // Build the RLT constraints.
        for (int i = 0; i < factors.size(); i++) {
            Factor facI = factors.get(i);
            for (int j = 0; j <= i; j++) {
                Factor facJ = factors.get(j);

                String rowName = String.format("cons_{%d, %d}", i, j);
                int rowind = rltMat.addRow(cplex.range(CPLEX_NEG_INF, 0.0, rowName));
                rltConsInd[i][j] = rowind;
                updateRowNZs(facJ, facI, rowind, rltMat, constantVarColIdx, rltVarsInd);
            }
        }

        return new RltProgram(rltMat, rltVars, inputMatrix, factors, constantVarColIdx, rltVarsInd, rltConsInd);
    }

    private static void updateRowNZs(Factor facJ, Factor facI, int rowind, IloLPMatrix rltMat,
            int constantVarColIdx, int[][] rltVarsInd) throws IloException {
        // Here we add the following constraint:
        // \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        // + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        // + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk}) w_{kl} &\leq g_ig_j

        SparseVector row = new FastSparseVector();
        // Part 1: \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        SparseVector facIG = facI.G.copy();
        SparseVector facJG = facJ.G.copy();
        row.add(facIG.scale(facJ.g));
        row.add(facJG.scale(facI.g));

        // Part 2: + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        SparseVector ip = facI.G.hadamardProd(facJ.G);
        ip = ip.scale(-1.0);
        SparseVector shiftedIp = new FastSparseVector();
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
        double rowUb = facI.g * facJ.g;                
        row.add(constantVarColIdx, -rowUb);
        rltMat.setNZs(getRowIndArray(row, rowind), row.getIndex(), row.getData());
        
        log.debug(rltMat.getRange(rowind).getName() + " " + row + " <= " + rowUb);
    }

    /**
     * Gets an int array of the same length as row.getIndex() and filled with rowind.
     */
    private static int[] getRowIndArray(SparseVector row, int rowind) {
        int[] array = new int[row.getIndex().length];
        Arrays.fill(array, rowind);
        return array;
    }

}
