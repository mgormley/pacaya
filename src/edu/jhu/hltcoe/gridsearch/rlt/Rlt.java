package edu.jhu.hltcoe.gridsearch.rlt;

import gnu.trove.TIntIntHashMap;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.FastSparseVector;
import no.uib.cipr.matrix.sparse.SparseVector;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.BoundFactor;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import edu.jhu.hltcoe.gridsearch.rlt.filter.RltFactorFilter;
import edu.jhu.hltcoe.gridsearch.rlt.filter.RltRowFilter;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.cplex.CplexRowUpdates;
import edu.jhu.hltcoe.util.cplex.CplexRows;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class Rlt {

    public static class RltPrm {
        public boolean envelopeOnly = true;
        public boolean nameRltVarsAndCons = true;
        public RltFactorFilter factorFilter = null;
        public RltRowFilter rowFilter = null;
        public RltRowFilter alwaysKeepRowFilter = null;
        public int maxRowsToCache = 10000;
        
        public static RltPrm getConvexConcaveEnvelope() {
            RltPrm prm = new RltPrm();
            prm.envelopeOnly = true;
            return prm;
        }

        public static RltPrm getFirstOrderRlt() {
            RltPrm prm = new RltPrm();
            prm.envelopeOnly = false;
            return prm;
        }
    }
    
    /**
     * This class caches rows so that they can be re-indexed and then pushed to
     * CPLEX in batches.
     */
    private class RltRows {

        private int maxRowsToCache;
        private CplexRows rows;
        private int numRowsAdded;
        private SymIntMat tempRltConsInd;
        
        public RltRows(int maxRowsToCache) {
            this.maxRowsToCache = maxRowsToCache;
            this.numRowsAdded = 0;
            reset();
        }

        private void reset() {
            this.rows = new CplexRows(prm.nameRltVarsAndCons);
            tempRltConsInd = new SymIntMat();
        }

        public void addRow(double lb, SparseVector coef, double ub, String name) throws IloException {
            rows.addRow(lb, coef, ub, name);
            numRowsAdded++;
            maybePush();
        }

        public void addRow(double cplexNegInf, SparseVector row, double d, String rowName, int i, int j) throws IloException {
            int rowind = rows.addRow(CplexUtils.CPLEX_NEG_INF, row, 0.0, rowName);
            tempRltConsInd.set(i, j, rowind);
            numRowsAdded++;
            maybePush();
        }
        
        private void maybePush() throws IloException {
            if (rows.getNumRows() >= maxRowsToCache) {
                pushRowsToCplex();
            }
        }

        public void pushRowsToCplex() throws IloException {
            log.trace("Converting RLT variable IDs to column indices.");
            convertRltVarIdsToColumIndices(rows);
            log.trace("Adding RLT constraints to matrix.");
            int startRow = rows.addRowsToMatrix(rltMat);
            tempRltConsInd.incrementAll(startRow);
            rltConsIdx.setAll(tempRltConsInd);
            log.debug("RLT rows added: " + numRowsAdded);
            reset();
        }

        public int getNumRows() {
            return numRowsAdded;
        }

    }

    private static final Logger log = Logger.getLogger(Rlt.class);

    private IloLPMatrix rltMat;
    IloLPMatrix inputMatrix;
    private List<Factor> factors;
    private HashMap<Pair<IloNumVar, Lu>, Integer> boundsFactorMap;
    private IloCplex cplex;
    private RltPrm prm;
    // The column index of the constant (always equal to 1.0) variable in the
    // RLT matrix.
    private int constantVarColIdx;

    // The INTERNAL column indices of the RLT vars, where rltVarsInd.get(i,j) := index of
    // w_{ij} in the RLT matrix.
    private SymIntMat rltVarsIdx;
    // Mapping of INDENTIFIERS for RLT matrix vars to INTERNAL column indices.
    private TIntIntHashMap idToColIdx;
    // The columns of the RLT matrix.
    private ArrayList<IloNumVar> rltMatVars;
    
    // The row indices of the RLT constraints, where rltConsInd.get(i,j) :=
    // index of the constraint formed by multiplying factors.get(i) *
    // factors.get(j). Only includes row indices for <= constraints, not 
    // for == constraints.
    private SymIntMat rltConsIdx;

    // The variables from the original inputMatrix.
    private IloNumVar[] numVars;

    private RltIds idsForRltVars;
    
    public Rlt(IloCplex cplex, IloLPMatrix inputMatrix, RltPrm prm) throws IloException {
        this.cplex = cplex;
        this.prm = prm;
        this.inputMatrix = inputMatrix;

        numVars = inputMatrix.getNumVars();

        List<Factor> newFactors = FactorBuilder.getFactors(inputMatrix, prm.envelopeOnly);
        log.debug("# unfiltered input variables: " + inputMatrix.getNcols());
        log.debug("# unfiltered RLT variables: " + inputMatrix.getNcols() * inputMatrix.getNcols());
        log.debug("# unfiltered input factors: " + newFactors.size());
        log.debug("# unfiltered RLT rows: " + FactorBuilder.getNumRows(newFactors, inputMatrix));
        
        // Reformulate and linearize the constraints.

        // Add the columns to the matrix.
        log.debug("Adding columns to RLT matrix.");
        rltMat = cplex.LPMatrix();
        // Add the input matrix's variables.
        rltMat.addCols(numVars);
        // Add a variable that is always 1.0. This enables us to update the
        // upper bound without adding/removing rows of the matrix.
        constantVarColIdx = rltMat.addColumn(cplex.numVar(1, 1, "const"));
        
        // Store the IloNumVar objects.
        rltMatVars = new ArrayList<IloNumVar>();
        rltMatVars.addAll(Arrays.asList(numVars));
        rltMatVars.add(rltMat.getNumVar(constantVarColIdx));

        // Setup the ID to column index map. Set the values for the non-RLT variables.
        idToColIdx = new TIntIntHashMap();
        for (int i=0; i<numVars.length; i++) {
            idToColIdx.put(i, i);
        }
        idToColIdx.put(constantVarColIdx, constantVarColIdx);
        idsForRltVars = new RltIds(numVars.length);
        
        // Create but do not fill the mapping from i,j pairs to column indices
        // in the RLT matrix.
        rltVarsIdx = new SymIntMat();

        // Initialize the filter with this RLT object.
        if (prm.factorFilter != null) {
            prm.factorFilter.init(this);
        }
        if (prm.rowFilter != null) {
            prm.rowFilter.init(this);
        }
        if (prm.alwaysKeepRowFilter != null && prm.rowFilter != prm.alwaysKeepRowFilter) {
            prm.alwaysKeepRowFilter.init(this);
        }
        
        // Store RLT constraints' row indices.
        rltConsIdx = new SymIntMat();

        // Build the RLT constraints by adding each factor one at a time.
        log.debug("Creating RLT constraints.");
        this.factors = new ArrayList<Factor>();
        addNewFactors(newFactors);
        
        log.debug("# filtered RLT factors: " + factors.size());
        log.debug("# filtered RLT rows: " + rltMat.getNrows());

        // This can be initialized only after all the bounds factors have been
        // added to the RLT matrix, rltMat.
        this.boundsFactorMap = getBoundsFactorMap(rltMat, factors);
    }

    private void convertRltVarIdsToColumIndices(CplexRows rows)  throws IloException {
        rows.setAllCoefs(convertRltVarIdsToColumIndices(rows.getAllCoefs()));
    }

    private void convertRltVarIdsToColumIndices(CplexRowUpdates rows) throws IloException {
        rows.setAllCoefs(convertRltVarIdsToColumIndices(rows.getAllCoefs()));
    }
    
    private ArrayList<SparseVector> convertRltVarIdsToColumIndices(List<SparseVector> rows)  throws IloException {
        // Make sure all the IDs are added as columns.
        log.trace("Creating new first-order RLT variables.");
        int numRltCols = rltMat.getNcols();
        List<IloNumVar> newRltVars = new ArrayList<IloNumVar>();
        for (int m=0; m<rows.size(); m++) {
            for (VectorEntry ve : rows.get(m)) {
                int id = ve.index();
                if (!idToColIdx.contains(id)) {
                    int i = idsForRltVars.getI(id);
                    int j = idsForRltVars.getJ(id);
                    
                    IloNumVar rltVar = cplex.numVar(CplexUtils.getLowerBound(numVars[i], numVars[j]), CplexUtils.getUpperBound(numVars[i], numVars[j]));
                    if (prm.nameRltVarsAndCons) {
                        // Optionally add a name.
                        rltVar.setName(String.format("w_{%s,%s}", numVars[i].getName(), numVars[j].getName()));
                    }
                    
                    // Add the new variable to the appropriate mappings and lists.
                    int index = numRltCols + newRltVars.size();
                    rltVarsIdx.set(i, j, index);
                    idToColIdx.put(id, index);
                    newRltVars.add(rltVar);
                }
            }
        }
        rltMat.addCols(newRltVars.toArray(new IloNumVar[]{}));
        rltMatVars.addAll(newRltVars);
        
        // Convert the IDs to indices.
        log.trace("Converting IDs to column indices");
        ArrayList<SparseVector> rowsWithColIdx = new ArrayList<SparseVector>();
        for (int m=0; m<rows.size(); m++) {
            SparseVector oldCoefs = rows.get(m);
            SparseVector newCoefs = new FastSparseVector();
            for (VectorEntry ve : oldCoefs) {
                int id = ve.index();
                int index = idToColIdx.get(id);
                assert (index != -1);
                newCoefs.set(index, ve.get());
            }
            rowsWithColIdx.add(newCoefs);
        }
        return rowsWithColIdx;
    }
    
    /**
     * Adds a list of factors by constructing a full set of rows and then adding the rows
     * all at once to the CPLEX modeling object.
     * @return The number of rows added to the RLT matrix.
     */
    private int addNewFactors(List<Factor> newFactors) throws IloException {
        RltRows rows = new RltRows(prm.maxRowsToCache );
        for (Factor factor : newFactors) {
            addFactor(cplex, factor, rows);
        }
        rows.pushRowsToCplex();
        return rows.getNumRows();
    }
    
    /**
     * Adds a new factor to the RLT program by appending new rows to the CplexRows object.
     */
    private void addFactor(IloCplex cplex, Factor facI, RltRows rows) throws IloException {
        if (prm.factorFilter != null && !prm.factorFilter.accept(facI)) {
            return;
        }
        int n = inputMatrix.getNcols();
        // Index of the factor we are adding.
        int i = factors.size();
        // Add the new factor so we multiply it with itself.
        this.factors.add(facI);
        if (facI.isEq()) {
            // Add an equality factor by multiplying it with each variable.
            for (int k = 0; k < n; k++) {
                // k is the column index of the kth column variable.
                if (log.isTraceEnabled()) {
                    assert (inputMatrix.getNumVar(k) == rltMat.getNumVar(k));
                }
                
                String rowName = prm.nameRltVarsAndCons ? String.format("eqcons_{%d,%d}", i, k) : null;
                SparseVector row = getRltRowForEq(facI, k, idsForRltVars);
                if (prm.rowFilter == null || prm.rowFilter.acceptEq(row, rowName, facI, k)
                        || (prm.alwaysKeepRowFilter != null && prm.alwaysKeepRowFilter.acceptEq(row, rowName, facI, k))) {
                    // Add the complete constraint.
                    rows.addRow(0.0, row, 0.0, rowName);
                }
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

                String rowName = prm.nameRltVarsAndCons ? String.format("lecons_{%s,%s}", facI.getName(), facJ.getName()) : null;
                SparseVector row = getRltRowForLeq(facJ, facI, constantVarColIdx, idsForRltVars);
                if (prm.rowFilter == null || prm.rowFilter.acceptLeq(row, rowName, facI, facJ)
                        || (prm.alwaysKeepRowFilter != null && prm.alwaysKeepRowFilter.acceptLeq(row, rowName, facI, facJ))) {
                    // Add the complete constraint.
                    rows.addRow(CplexUtils.CPLEX_NEG_INF, row, 0.0, rowName, i, j);
                }
            }
        }
    }

    public IloLPMatrix getRltMatrix() {
        return rltMat;
    }

    public IloLPMatrix getInputMatrix() {
        return inputMatrix;
    }

    public IloNumVar getRltVar(IloNumVar var1, IloNumVar var2) throws IloException {
        int idx1 = inputMatrix.getIndex(var1);
        int idx2 = inputMatrix.getIndex(var2);
        
        int id = idsForRltVars.get(idx1, idx2);
        int index = idToColIdx.get(id);
        return rltMatVars.get(index);
    }

    /**
     * Gets the column index for the RLT variable formed by the product var1 * var2.
     */
    public int getRltVarIdx(IloNumVar var1, IloNumVar var2) throws IloException {
        int idx1 = inputMatrix.getIndex(var1);
        int idx2 = inputMatrix.getIndex(var2);
        return rltVarsIdx.get(idx1, idx2);
    }
    
    /**
     * Gets the identifier for the RLT variable formed by the product var1 * var2.
     */
    public int getIdForRltVar(IloNumVar var1, IloNumVar var2) throws IloException {
        int idx1 = inputMatrix.getIndex(var1);
        int idx2 = inputMatrix.getIndex(var2);
        return idsForRltVars.get(idx1, idx2);
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
            factor = FactorBuilder.getBoundFactorLower(inputMatrix.getNumVars(), bf.colIdx, inputMatrix);
        } else {
            factor = FactorBuilder.getBoundFactorUpper(inputMatrix.getNumVars(), bf.colIdx, inputMatrix);
        }
        factors.set(factorIdx, factor);

        // For each row:
        // - Update the coefficients.
        // - Update the upper bound.
        // This requires adding an auxiliary variable that is fixed to equal
        // 1.0.
        CplexRowUpdates rows = new CplexRowUpdates();
        for (int i = 0; i < factors.size(); i++) {
            Factor facI = factors.get(i);
            if (rltConsIdx.contains(factorIdx, i)) {
                // A bound will have only been added if it passed the filter. 
                SparseVector row = getRltRowForLeq(factor, facI, constantVarColIdx, idsForRltVars);
                int rowind = rltConsIdx.get(factorIdx, i);
                rows.add(rowind, row);
            }
        }
        convertRltVarIdsToColumIndices(rows);
        rows.updateRowsInMatrix(rltMat);
    }

    private int getFactorIdx(IloNumVar var, Lu lu) throws IloException {
        int fIdx = boundsFactorMap.get(new Pair<IloNumVar, Lu>(var, lu));
        BoundFactor bf = (BoundFactor) factors.get(fIdx);
        assert (bf.lu == lu);
        if (log.isTraceEnabled()) {
            assert (bf.colIdx == rltMat.getIndex(var));
        }
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

    /**
     * For testing only.
     */
    public double[][] getRltVarVals(IloCplex cplex) throws IloException {
        int n = rltMat.getNcols();
        IloNumVar[] allVars = rltMat.getNumVars();
        // Create the first-order RLT variables.
        SymVarMat rltVars = new SymVarMat();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                if (rltVarsIdx.contains(i,j)) {
                    rltVars.set(i, j, allVars[rltVarsIdx.get(i, j)]);
                }
            }
        }
        return CplexUtils.getValues(cplex, rltVars);
    }

    /**
     * @return The number of rows added to the RLT matrix.
     */
    public int addRows(List<Integer> rowIds) throws IloException {
        if (prm.envelopeOnly) {
            // Don't add the rows.
            return 0;
        }
        if (!areConsecutive(rowIds)) {
            throw new IllegalStateException("Expecting a consecutive list: " + rowIds);
        }
        if (rowIds.size() > 0) {
            List<Factor> newFactors = new ArrayList<Factor>();
            FactorBuilder.addRowFactors(rowIds.get(0), rowIds.size(), inputMatrix, newFactors);
            return addNewFactors(newFactors);
        } else {
            return 0;
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

    private static SparseVector getRltRowForLeq(Factor facJ, Factor facI, int constantVarColIdx, RltIds rltVarsInd)
            throws IloException {
        // Here we add the following constraint:
        // \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        // + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        // + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk})
        // w_{kl} &\leq g_ig_j

        FastSparseVector row = new FastSparseVector();
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
        row.add(shiftedIp);

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

    private static SparseVector getRltRowForEq(Factor facI, int k, RltIds rltVarsInd) throws IloException {
        FastSparseVector row = new FastSparseVector();

        // Original: x_k * g_i + sum_{l=1}^n - x_k * G_{il} * x_l = 0
        // Linearized: x_k * g_i + sum_{l=1}^n - G_{il} w_{kl} = 0

        // Add x_k * g_i
        row.add(k, facI.g);

        // Add sum_{l=1}^n - G_{il} w_{kl}
        for (int idx = 0; idx < facI.G.getUsed(); idx++) {
            int l = facI.G.getIndex()[idx];
            double val = - facI.G.getData()[idx];
            row.add(rltVarsInd.get(k, l), val);
        }
        return row;
    }
}
