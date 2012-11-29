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
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.BoundFactor;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymIntMat;
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import edu.jhu.hltcoe.util.CplexUtils;
import edu.jhu.hltcoe.util.Pair;

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

    public static class RltParams {
        public boolean envelopeOnly = true;
        public RltRowFilter filter = null;

        public static RltParams getConvexConcaveEnvelope() {
            RltParams prm = new RltParams();
            prm.envelopeOnly = true;
            return prm;
        }

        public static RltParams getFirstOrderRlt() {
            RltParams prm = new RltParams();
            prm.envelopeOnly = false;
            return prm;
        }

    }

    public static class RltRowFilter {

    }

    private IloLPMatrix rltMat;
    private SymVarMat rltVars;
    private IloLPMatrix inputMatrix;
    private List<Factor> factors;
    private int constantVarColIdx;
    private SymIntMat rltVarsInd;
    private SymIntMat rltConsInd;
    private HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap;
    private IloCplex cplex;
    private RltParams prm;

    public Rlt(IloCplex cplex, IloLPMatrix inputMatrix, RltParams prm) throws IloException {
        List<Factor> newFactors = FactorBuilder.getFactors(inputMatrix, prm.envelopeOnly);
        log.debug("RLT factors: " + newFactors.size());

        this.cplex = cplex;
        this.prm = prm;
        this.inputMatrix = inputMatrix;

        int n = inputMatrix.getNcols();
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
                rltVarsInd.set(i, j, rltMat.getIndex(rltVars.get(i, j)));
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
            for (int k = 0; k < n; k++) {
                String rowName = String.format("eqcons_{%d, %d}", i, k);
                SparseVector row = getRltRowForEq(facI, k, rltVarsInd);

                // k is the column index of the kth column variable.
                assert (inputMatrix.getNumVar(k) == rltMat.getNumVar(k));

                // Add the complete constraint.
                int rowind = rltMat.addRow(cplex.range(0.0, 0.0, rowName));
                updateRow(rowind, row);
            }
        } else {
            // Add a standard <= factor by multiplying it with each factor.
            for (int j = 0; j < factors.size(); j++) {
                Factor facJ = factors.get(j);

                if (facI.isEq() || facJ.isEq()) {
                    // Skip the equality constraints.
                    continue;
                } else if (prm.envelopeOnly && ((BoundFactor) facI).colIdx == ((BoundFactor) facJ).colIdx) {
                    // Don't multiply the constraint with itself for the
                    // envelope.
                    continue;
                }

                String rowName = String.format("lecons_{%d, %d}", i, j);
                SparseVector row = getRltRowForLeq(facJ, facI, constantVarColIdx, rltVarsInd);

                // Add the complete constraint.
                int rowind = rltMat.addRow(cplex.range(CPLEX_NEG_INF, 0.0, rowName));
                rltConsInd.set(i, j, rowind);
                updateRow(rowind, row);
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

    /**
     * Recreate the RLT constraints that used the bounds factor for the
     * specified variable and bound type (upper/lower).
     */
    public void updateBound(IloNumVar var, Lu lu) throws IloException {
        // Find the factor corresponding to this variable and lower/upper bound.
        int factorIdx = getFactorIdx(var, lu);

        // Update the factor.
        BoundFactor bf = (BoundFactor) factors.get(factorIdx);
        Factor factor;
        if (bf.lu == Lu.LOWER) {
            factor = FactorBuilder.getBoundFactorLower(inputMatrix.getNumVars(), bf.colIdx);
        } else {
            factor = FactorBuilder.getBoundFactorUpper(inputMatrix.getNumVars(), bf.colIdx);
        }
        factors.set(factorIdx, factor);

        // For each row:
        // - Update the coefficients.
        // - Update the upper bound.
        // This requires adding an auxiliary variable that is fixed to equal
        // 1.0.
        for (int i = 0; i < factors.size(); i++) {
            Factor facI = factors.get(i);
            if (rltConsInd.contains(factorIdx, i)) {
                SparseVector row = getRltRowForLeq(factor, facI, constantVarColIdx, rltVarsInd);
                int rowind = rltConsInd.get(factorIdx, i);
                updateRow(rowind, row);
            }
        }
    }

    /**
     * Update an existing RLT row.
     */
    private void updateRow(int rowind, SparseVector row) throws IloException {
        // Add the complete constraint.
        log.trace(rltMat.getRange(rowind).getName() + " " + row + " <= " + 0.0);
        rltMat.setNZs(getRowIndArray(row, rowind), row.getIndex(), row.getData());
    }

    private int getFactorIdx(IloNumVar var, Lu lu) throws IloException {
        int fIdx = boundsFactorMap.get(new Pair<IloNumVar, Lu>(var, lu));
        BoundFactor bf = (BoundFactor) factors.get(fIdx);
        assert (bf.lu == lu);
        assert (bf.colIdx == rltMat.getIndex(var));
        return fIdx;
    }

    private static HashMap<Pair<IloNumVar, Lu>, Integer> getBoundsFactorMap(IloLPMatrix rltMat, List<Factor> factors)
            throws IloException {
        HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap = new HashMap<Pair<IloNumVar, Lu>, Integer>();
        for (int i = 0; i < factors.size(); i++) {
            Factor f = factors.get(i);
            if (f instanceof BoundFactor) {
                BoundFactor bf = (BoundFactor) f;
                boundsFactorMap.put(new Pair<IloNumVar, Lu>(rltMat.getNumVar(bf.colIdx), bf.lu), i);
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
            FactorBuilder.addRowFactors(rows.get(0), rows.size(), inputMatrix, newFactors);
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
        int i = 0;
        int cur = rows.get(i);
        for (i = 1; i < rows.size(); i++) {
            if (rows.get(i) != cur + 1) {
                return false;
            } else {
                cur = rows.get(i);
            }
        }
        return true;
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

    private static SparseVector getRltRowForLeq(Factor facJ, Factor facI, int constantVarColIdx, SymIntMat rltVarsInd)
            throws IloException {
        // Here we add the following constraint:
        // \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        // + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        // + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk})
        // w_{kl} &\leq g_ig_j

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
            shiftedIp.set(rltVarsInd.get(k, k), val);
        }
        row = (SparseVector) row.add(shiftedIp);

        // Part 3: + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il}
        // G_{jk}) w_{kl}
        for (int ii = 0; ii < facI.G.getUsed(); ii++) {
            int k = facI.G.getIndex()[ii];
            double vi = facI.G.getData()[ii];
            for (int jj = 0; jj < facJ.G.getUsed(); jj++) {
                int l = facJ.G.getIndex()[jj];
                double vj = facJ.G.getData()[jj];
                if (k == l) {
                    continue;
                }
                row.add(rltVarsInd.get(k, l), -vi * vj);
            }
        }

        double rowUb = facI.g * facJ.g;
        row.add(constantVarColIdx, -rowUb);
        return row;
    }

    private static SparseVector getRltRowForEq(Factor facI, int k, SymIntMat rltVarsInd) throws IloException {
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
        return row;
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
