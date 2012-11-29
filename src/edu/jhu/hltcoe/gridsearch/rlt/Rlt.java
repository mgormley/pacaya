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
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymIntMat;
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.CplexUtils;
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
        private SymVarMat rltVars;
        private IloLPMatrix inputMatrix;
        private List<Factor> factors;
        private int constantVarColIdx;
        private SymIntMat rltVarsInd;
        private SymIntMat rltConsInd;
        private HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap;
        private IloCplex cplex;
        private boolean envelopeOnly;

        private RltProgram(IloCplex cplex, List<Factor> newFactors, IloLPMatrix inputMatrix, boolean envelopeOnly)
                throws IloException {
            log.debug("RLT factors: " + newFactors.size());

            this.cplex = cplex;
            this.envelopeOnly = envelopeOnly;
            this.inputMatrix = inputMatrix;
            
            int n = inputMatrix.getNcols();
            int m = inputMatrix.getNrows();
            IloNumVar[] numVars = inputMatrix.getNumVars();
            
            // Create the first-order RLT variables.
            rltVars = new SymVarMat();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= i; j++) {
                    rltVars.set(i, j, cplex.numVar(getLowerBound(numVars[i], numVars[j]), getUpperBound(numVars[i],
                            numVars[j]), String.format("w_{%d,%d}", i, j)));
                }
            }
            
            // Reformulate and linearize the constraints.

            // Add the columns to the matrix.
            rltMat = cplex.LPMatrix();
            rltMat.addCols(numVars);
            for (int i = 0; i < n; i++) {
                rltMat.addCols(rltVars.getRowAsArray(i));
            }

            // Add a variable that is always 1.0. This enables us to update the
            // upper bound without adding/removing rows of the matrix.
            constantVarColIdx = rltMat.addColumn(cplex.numVar(1, 1, "const"));
            
            // Store rltVar column indices.
            rltVarsInd = new SymIntMat();
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= i; j++) {
                    rltVarsInd.set(i, j, rltMat.getIndex(rltVars.get(i,j)));
                }
            }
            
            // Store RLT constraints' row indices.
            rltConsInd = new SymIntMat();

            // Build the RLT constraints by adding each factor one at a time.
            this.factors = new ArrayList<Factor>();
            for (int i = 0; i < newFactors.size(); i++) {
                addFactor(cplex, newFactors.get(i));
            }            

            // This can be initialized only after all the bounds factors have been
            // added to the RLT matrix, rltMat.
            this.boundsFactorMap = getBoundsFactorMap(rltMat, factors);
        }
        
        private void addFactor(IloCplex cplex, Factor facI) throws IloException {
            int n = inputMatrix.getNcols();
            // Index of the factor we are adding.
            int i = factors.size();
            // Add the new factor so we multiply it with itself.
            this.factors.add(facI);
            if (facI.isEq()) {  
                // Add an equality factor by multiplying it with each variable.
                for (int k=0; k<n; k++) {
                    String rowName = String.format("eqcons_{%d, %d}", i, k);
                    int rowind = rltMat.addRow(cplex.range(0.0, 0.0, rowName));
                    // k is the column index of the kth column variable.
                    assert(inputMatrix.getNumVar(k) == rltMat.getNumVar(k));
                    
                    updateRowNZsForEq(facI, k, rowind, rltMat, rltVarsInd);
                }
            } else {
                // Add a standard <= factor by multiplying it with each factor.
                for (int j = 0; j < factors.size(); j++) {
                    Factor facJ = factors.get(j);
                    
                    if (facI.isEq() || facJ.isEq()) { 
                        // Skip the equality constraints.
                        continue;
                    } else if (envelopeOnly && ((BoundFactor)facI).colIdx == ((BoundFactor)facJ).colIdx) {
                        // Don't multiply the constraint with itself for the envelope. 
                        continue;
                    }
                    
                    String rowName = String.format("lecons_{%d, %d}", i, j);
                    int rowind = rltMat.addRow(cplex.range(CPLEX_NEG_INF, 0.0, rowName));
                    rltConsInd.set(i, j, rowind);
                    updateRowNZsForLeq(facJ, facI, rowind, rltMat, constantVarColIdx, rltVarsInd);
                }
            }
        }

        public IloLPMatrix getRltMatrix() {
            return rltMat;
        }

        public IloNumVar getRltVar(IloNumVar var1, IloNumVar var2) throws IloException {
            int idx1 = inputMatrix.getIndex(var1);
            int idx2 = inputMatrix.getIndex(var2);            
            return rltVars.get(idx1, idx2);
        }
        
        public void updateBound(IloNumVar var, Lu lu) throws IloException {
            // Find the factor corresponding to this variable and lower/upper bound.
            int factorIdx = getFactorIdx(var, lu);
            
            // Update the factor.
            BoundFactor bf = (BoundFactor)factors.get(factorIdx);
            Factor factor;
            if (bf.lu == Lu.LOWER) { 
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
                if (rltConsInd.contains(factorIdx, i)) {
                    int rowind = rltConsInd.get(factorIdx, i);
                    updateRowNZsForLeq(factor, facI, rowind, rltMat, constantVarColIdx, rltVarsInd);
                }
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

        public double[][] getRltVarVals(IloCplex cplex) throws IloException {
            return CplexUtils.getValues(cplex, rltVars);
        }

        public void addRows(List<Integer> rows) throws IloException {
            if (!areConsecutive(rows)) {
                throw new IllegalStateException("Expecting a consecutive list: " + rows);
            }
            if (rows.size() > 0) {
                List<Factor> newFactors = new ArrayList<Factor>();
                addRowFactors(rows.get(0), rows.size(), inputMatrix, newFactors);
                for (Factor factor : newFactors) {
                    addFactor(cplex, factor);
                }
            }
        }

        /**
         * Returns true iff the list of integers is a consecutive list.
         */
        private static boolean areConsecutive(List<Integer> rows) {
            if (rows.size() <= 1) {
                return true;
            }
            int i=0;
            int cur = rows.get(i);
            for (i=1; i<rows.size(); i++) {
                if (rows.get(i) != cur + 1) {
                    return false;
                } else {
                    cur = rows.get(i);
                }                
            }
            return true;
        }
        
    }

    private abstract static class Factor {
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
        
        public abstract boolean isEq();
    }
    
    private static enum RowFactorType {
        LOWER, EQ, UPPER
    }
    
    private static class RowFactor extends Factor {
        int rowIdx;
        RowFactorType type;
        public RowFactor(double g, int[] Gind, double[] Gval, int rowIdx, RowFactorType type) {
            super(g, Gind, Gval);
            this.rowIdx = rowIdx;
            this.type = type;
        }
        public boolean isEq() {
            return type == RowFactorType.EQ;
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
        public boolean isEq() {
            // TODO: we could allow equality constraints for the bounds, but since 
            // we also want to be able to update them, this gets tricky.
            return false;
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
        boolean envelopeOnly = true;
        List<Factor> factors = getFactors(mat, envelopeOnly);
        return new RltProgram(cplex, factors, mat, envelopeOnly);
    }

    public static RltProgram getFirstOrderRlt(IloCplex cplex, IloLPMatrix mat) throws IloException {
        boolean envelopeOnly = false;
        List<Factor> factors = getFactors(mat, envelopeOnly);
        return new RltProgram(cplex, factors, mat, envelopeOnly);
    }

    /**
     * Creates the constraint and bounds factors.
     */
    private static List<Factor> getFactors(IloLPMatrix mat, boolean envelopeOnly)
            throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();
        
        List<Factor> factors = new ArrayList<Factor>();
        
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

        if (!envelopeOnly) {
            // Add constraint factors.
            int startRow = 0;
            int numRows = m;            
            addRowFactors(startRow, numRows, mat, factors);
        }
        
        if (log.isTraceEnabled()) {
            log.trace("factors: ");
            for (Factor f : factors) {
                log.trace("\t" + f);
            }
        }
        return factors;
    }

    /**
     * Create numRows RowFactors starting from the startRow'th row of mat and add these rows to factors.
     * @return The number of new factors added to factors.
     */
    private static int addRowFactors(int startRow, int numRows, IloLPMatrix mat, List<Factor> factors)
            throws IloException {
        int numNewFactors = 0;
        double[] lb = new double[numRows];
        double[] ub = new double[numRows];
        int[][] Aind = new int[numRows][];
        double[][] Aval = new double[numRows][];
        mat.getRows(startRow, numRows, lb, ub, Aind, Aval);
        for (int rowIdx = 0; rowIdx < numRows; rowIdx++) {
            if (lb[rowIdx] == ub[rowIdx]) {
                // Special handling of equality constraints.
                factors.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.EQ));
                numNewFactors++;
            } else {
                if (lb[rowIdx] != CPLEX_NEG_INF) {
                    // b <= A_i x
                    // 0 <= A_i x - b = (-b - (-A_i x))
                    double[] vals = Utilities.copyOf(Aval[rowIdx]);
                    Vectors.scale(vals, -1.0);
                    factors.add(new RowFactor(-lb[rowIdx], Aind[rowIdx], vals, rowIdx, RowFactorType.LOWER));
                    numNewFactors++;
                }
                if (ub[rowIdx] != CPLEX_POS_INF) {
                    // A_i x <= b
                    // 0 <= b - A_i x
                    factors.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.UPPER));
                    numNewFactors++;
                }
            }
        }
        return numNewFactors;
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
    
    /**
     * Gets the lower bound of the product of two variables.
     */
    private static double getLowerBound(IloNumVar iloNumVar, IloNumVar iloNumVar2) {
        return CPLEX_NEG_INF;
    }

    /**
     * Gets the upper bound of the product of two variables.
     */
    private static double getUpperBound(IloNumVar iloNumVar, IloNumVar iloNumVar2) {
        return CPLEX_POS_INF;
    }

    private static void updateRowNZsForLeq(Factor facJ, Factor facI, int rowind, IloLPMatrix rltMat,
            int constantVarColIdx, SymIntMat rltVarsInd) throws IloException {
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
            shiftedIp.set(rltVarsInd.get(k,k), val);
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
                row.add(rltVarsInd.get(k,l), -vi * vj);
            }
        }
        
        // Add the complete constraint.
        double rowUb = facI.g * facJ.g;                
        row.add(constantVarColIdx, -rowUb);
        rltMat.setNZs(getRowIndArray(row, rowind), row.getIndex(), row.getData());
        
        log.trace(rltMat.getRange(rowind).getName() + " " + row + " <= " + rowUb);
    }

    private static void updateRowNZsForEq(Factor facI, int k, int rowind, IloLPMatrix rltMat, SymIntMat rltVarsInd)
            throws IloException {
        SparseVector row = new FastSparseVector();
        
        // Original: x_k * g_i + sum_{l=1}^n x_k * G_{il} * x_l
        // Linearized: x_k * g_i + sum_{l=1}^n G_{il} w_{kl}
        
        // Add x_k * g_i 
        row.add(k, facI.g);
        
        // Add sum_{l=1}^n G_{il} w_{kl}
        for (int idx = 0; idx < facI.G.getUsed(); idx++) {
            int l = facI.G.getIndex()[idx];
            double val = facI.G.getData()[idx];
            row.add(rltVarsInd.get(k, l), val);
        }
        
        // Add the complete constraint.
        rltMat.setNZs(getRowIndArray(row, rowind), row.getIndex(), row.getData());
        
        log.trace(rltMat.getRange(rowind).getName() + " " + row + " == 0.0");
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
