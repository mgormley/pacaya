package edu.jhu.gridsearch.rlt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import edu.jhu.gridsearch.rlt.filter.AllRltRowAdder;
import edu.jhu.gridsearch.rlt.filter.RltFactorFilter;
import edu.jhu.gridsearch.rlt.filter.RltRowAdder;
import edu.jhu.gridsearch.rlt.filter.RowType;
import edu.jhu.lp.FactorBuilder;
import edu.jhu.lp.FactorBuilder.BoundFactor;
import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorList;
import edu.jhu.lp.LpMatrixUpdates;
import edu.jhu.lp.LpRows;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.map.LongDoubleEntry;
import edu.jhu.prim.map.LongIntHashMap;
import edu.jhu.prim.vector.LongDoubleSortedVector;
import edu.jhu.util.Pair;
import edu.jhu.util.SafeCast;
import edu.jhu.util.cplex.CplexUtils;
import edu.jhu.util.tuple.OrderedPair;
import edu.jhu.util.tuple.UnorderedPair;

public class Rlt {

    public static class RltPrm {
        public boolean envelopeOnly = true;
        public RltFactorFilter factorFilter = null;
        public RltRowAdder rowAdder = new AllRltRowAdder();
        public boolean nameRltVarsAndCons = true;
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
        private LpRows rows;
        private int numRowsAdded;
        private SymIntMat tempRltConsInd;
        
        public RltRows(int maxRowsToCache) {
            this.maxRowsToCache = maxRowsToCache;
            this.numRowsAdded = 0;
            reset();
        }

        private void reset() {
            this.rows = new LpRows(prm.nameRltVarsAndCons);
            tempRltConsInd = new SymIntMat();
        }

        public void addRow(double lb, LongDoubleSortedVector coef, double ub, String name) throws IloException {
            rows.addRow(lb, coef, ub, name);
            numRowsAdded++;
            maybePush();
        }

        public void addRow(double cplexNegInf, LongDoubleSortedVector row, double d, String rowName, int i, int j) throws IloException {
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
            // Update the constraint mapping stored in the parent Rlt object. 
            rltLeqConsIdx.setAll(tempRltConsInd);
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
    private FactorList eqFactors;
    private FactorList leqFactors;
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
    private LongIntHashMap idToColIdx;
    // The columns of the RLT matrix.
    private ArrayList<IloNumVar> rltMatVars;
    
    // The row indices of the RLT constraints, where rltConsInd.get(i,j) :=
    // index of the constraint formed by multiplying factors.get(i) *
    // factors.get(j). Only includes row indices for <= constraints, not 
    // for == constraints.
    private SymIntMat rltLeqConsIdx;

    // The variables from the original inputMatrix.
    private IloNumVar[] numVars;

    private RltIds idsForRltVars;
    
    public Rlt(IloCplex cplex, IloLPMatrix inputMatrix, RltPrm prm) throws IloException {
        this.cplex = cplex;
        this.prm = prm;
        this.inputMatrix = inputMatrix;

        numVars = inputMatrix.getNumVars();

        log.debug("# unfiltered input variables: " + inputMatrix.getNcols());
        log.debug("# unfiltered RLT variables: " + (long) inputMatrix.getNcols() * inputMatrix.getNcols());
        
        // Reformulate and linearize the constraints.

        // Add the columns to the matrix.
        log.debug("Adding columns to RLT matrix.");
        rltMat = cplex.LPMatrix("rltMatrix");
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
        idToColIdx = new LongIntHashMap();
        for (int i=0; i<numVars.length; i++) {
            idToColIdx.put(i, i);
        }
        idToColIdx.put(constantVarColIdx, constantVarColIdx);
        idsForRltVars = new RltIds(numVars.length);
        
        // Create but do not fill the mapping from i,j pairs to column indices
        // in the RLT matrix.
        rltVarsIdx = new SymIntMat();

        // Store <= RLT constraints' row indices.
        rltLeqConsIdx = new SymIntMat();

        // Initialize the factor filter with this RLT object.
        if (prm.factorFilter != null) {
            prm.factorFilter.init(this);
        }

        // Create the factors.
        log.debug("Building RLT bound and constraint factors.");
        FactorList newFactors = FactorList.getFactors(inputMatrix, prm.envelopeOnly);
        log.debug("# unfiltered input factors: " + newFactors.size());
        long numUnfilteredRows = Rlt.getNumRltRows(newFactors, inputMatrix);
        log.debug("# unfiltered RLT rows: " + numUnfilteredRows);

        // Initialize the row adder with this RLT object.
        if (prm.rowAdder != null) {
            prm.rowAdder.init(this, numUnfilteredRows);
        }

        // Add each new factor, possibly filtering unwanted ones.
        this.eqFactors = new FactorList();
        this.leqFactors = new FactorList();
        // Build the RLT constraints by adding each factor one at a time.
        log.debug("Creating RLT constraints.");
        addNewFactors(newFactors, RowType.INITIAL);
        log.debug("# filtered RLT factors: " + (eqFactors.size() + leqFactors.size()));
        log.debug("# filtered RLT rows: " + rltMat.getNrows());

        // This can be initialized only after all the bounds factors have been
        // added.
        this.boundsFactorMap = getBoundsFactorMap(rltMat, leqFactors);
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
        
        long id = idsForRltVars.get(idx1, idx2);
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
    public long getIdForRltVar(IloNumVar var1, IloNumVar var2) throws IloException {
        int idx1 = inputMatrix.getIndex(var1);
        int idx2 = inputMatrix.getIndex(var2);
        return idsForRltVars.get(idx1, idx2);
    }

    /**
     * Gets the identifier for the input variable corresponding to a column in the input matrix.
     */
    public long getIdForInputVar(IloNumVar var) throws IloException {
        return inputMatrix.getIndex(var);
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
     * 
     * @param newFactors
     * @param type
     * @return The number of rows added to the RLT matrix.
     */
    private int addNewFactors(FactorList newFactors, RowType type) throws IloException {
        int eqStart = eqFactors.size();
        int leqStart = leqFactors.size();        
        for (Factor factor : newFactors) {
            if (prm.factorFilter == null || prm.factorFilter.accept(factor)) {
                if (factor.isEq()) {
                    this.eqFactors.add(factor);
                } else {
                    this.leqFactors.add(factor);
                }
            }
        }
        int eqEnd = eqFactors.size();
        int leqEnd = leqFactors.size();
        
        int n = inputMatrix.getNcols();

        // Add the RLT rows by constructing a full set of rows and then adding the rows
        // all at once to the CPLEX modeling object.
        RltRows rows = new RltRows(prm.maxRowsToCache);
        if (prm.rowAdder != null) {
            for (OrderedPair fvp : prm.rowAdder.getRltRowsForEq(eqStart, eqEnd, n, type)) {
                // Add an equality factor by multiplying factor i it with each variable k.
                addRltRowForEq(fvp.get1(), fvp.get2(), rows);
            }
            for (UnorderedPair up : prm.rowAdder.getRltRowsForLeq(0, leqFactors.size(), leqStart, leqEnd, type)) {
                // Add a standard <= factor by multiplying factor i with factor j.
                addRltRowForLeq(up.get1(), up.get2(), rows);
            }
        }
        rows.pushRowsToCplex();
        return rows.getNumRows();
    }

    private void addRltRowForEq(int i, int k, RltRows rows) throws IloException {
        Factor facI = eqFactors.get(i);
        if (!facI.isEq()) {
            throw new IllegalStateException("The index i must correspond to an equality factor.");
        } else if (k < 0 || k >= numVars.length) {
            throw new IllegalStateException("The index k must correspond to an input variable.");
        }
        
        // k is the column index of the kth column variable.
        if (log.isTraceEnabled()) {
            assert (inputMatrix.getNumVar(k) == rltMat.getNumVar(k));
        }
        
        String rowName = prm.nameRltVarsAndCons ? String.format("eqcons_{%s,%s}", facI.getName(), inputMatrix.getNumVar(k).getName()) : null;
        LongDoubleSortedVector row = getRltRowForEq(facI, k, idsForRltVars);
        // Add the complete constraint.
        rows.addRow(0.0, row, 0.0, rowName);
    }

    // TODO: check for duplicates?
    private void addRltRowForLeq(int iIdx, int jIdx, RltRows rows) throws IloException {
        int i = Math.min(iIdx, jIdx);
        int j = Math.max(iIdx, jIdx);
        Factor facI = leqFactors.get(i);
        Factor facJ = leqFactors.get(j);

        if (facI.isEq() || facJ.isEq()) {
            throw new IllegalStateException("The indices i and j must correspond to inequality factors.");
        } else if (prm.envelopeOnly && ((BoundFactor) facI).colIdx == ((BoundFactor) facJ).colIdx) {
            // Don't multiply the constraint with itself for the
            // envelope.
            return;
        } else if (rltLeqConsIdx.contains(i, j)) {
            // TODO: this won't catch any duplicates within this set.
            log.warn(String.format("Attempt to add duplicate row (%d, %d). Skipping.", i, j));
            return;
        }

        String rowName = prm.nameRltVarsAndCons ? String.format("lecons_{%s,%s}", facI.getName(), facJ.getName()) : null;
        LongDoubleSortedVector row = getRltRowForLeq(facJ, facI, constantVarColIdx, idsForRltVars);
        // Add the complete constraint.
        rows.addRow(CplexUtils.CPLEX_NEG_INF, row, 0.0, rowName, i, j);
    }

    /**
     * Recreate the RLT constraints that used the bounds factor for the
     * specified variable and bound type (upper/lower).
     */
    public void updateBound(IloNumVar var, Lu lu) throws IloException {
        // Find the factor corresponding to this variable and lower/upper bound.
        int factorIdx = getFactorIdx(var, lu);

        // Update the factor.
        BoundFactor origBf = (BoundFactor) leqFactors.get(factorIdx);
        Factor newBj;
        if (origBf.lu == Lu.LOWER) {
            newBj = FactorBuilder.getBoundFactorLower(numVars, origBf.colIdx, inputMatrix);
        } else {
            newBj = FactorBuilder.getBoundFactorUpper(numVars, origBf.colIdx, inputMatrix);
        }
        leqFactors.set(factorIdx, newBj);

        // For each row:
        // - Update the coefficients.
        // - Update the upper bound.
        // This requires adding an auxiliary variable that is fixed to equal
        // 1.0.
        LpMatrixUpdates rows = new LpMatrixUpdates();
        for (int i = 0; i < leqFactors.size(); i++) {
            Factor facI = leqFactors.get(i);
            if (rltLeqConsIdx.contains(factorIdx, i)) {
                // A bound will have only been added if it passed the filter. 
                LongDoubleSortedVector row = getRltRowForLeq(newBj, facI, constantVarColIdx, idsForRltVars);
                int rowind = rltLeqConsIdx.get(factorIdx, i);
                rows.add(rowind, row);
            }
        }
        convertRltVarIdsToColumIndices(rows);
        rows.updateRowsInMatrix(rltMat);
    }
    
    /**
     * @return The number of rows added to the RLT matrix.
     */
    public int addRowsAsFactors(IntArrayList rowIds) throws IloException {
        if (prm.envelopeOnly) {
            // Don't add the rows.
            return 0;
        }
        if (!areConsecutive(rowIds)) {
            throw new IllegalStateException("Expecting a consecutive list: " + rowIds);
        }
        if (rowIds.size() > 0) {
            FactorList newFactors = new FactorList();
            newFactors.addRowFactors(rowIds.get(0), rowIds.size(), inputMatrix);
            return addNewFactors(newFactors, RowType.CUT);
        } else {
            return 0;
        }
    }

    private void convertRltVarIdsToColumIndices(LpRows rows)  throws IloException {
        rows.setAllCoefs(convertRltVarIdsToColumIndices(rows.getAllCoefs()));
    }

    private void convertRltVarIdsToColumIndices(LpMatrixUpdates rows) throws IloException {
        rows.setAllCoefs(convertRltVarIdsToColumIndices(rows.getAllCoefs()));
    }
    
    private ArrayList<LongDoubleSortedVector> convertRltVarIdsToColumIndices(List<LongDoubleSortedVector> rows)  throws IloException {
        // Make sure all the IDs are added as columns.
        log.trace("Creating new first-order RLT variables.");
        int numRltCols = rltMat.getNcols();
        List<IloNumVar> newRltVars = new ArrayList<IloNumVar>();
        for (int m=0; m<rows.size(); m++) {
            for (LongDoubleEntry ve : rows.get(m)) {
                long id = ve.index();
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
        ArrayList<LongDoubleSortedVector> rowsWithColIdx = new ArrayList<LongDoubleSortedVector>();
        for (int m=0; m<rows.size(); m++) {
            LongDoubleSortedVector oldCoefs = rows.get(m);
            // TODO: This should really be a FastSparseVector without longs.
            LongDoubleSortedVector newCoefs = new LongDoubleSortedVector();
            for (LongDoubleEntry ve : oldCoefs) {
                long id = ve.index();
                int index = idToColIdx.get(id);
                assert (index != -1);
                newCoefs.set(index, ve.get());
            }
            rowsWithColIdx.add(newCoefs);
        }
        return rowsWithColIdx;
    }

    private int getFactorIdx(IloNumVar var, Lu lu) throws IloException {
        int fIdx = boundsFactorMap.get(new Pair<IloNumVar, Lu>(var, lu));
        BoundFactor bf = (BoundFactor) leqFactors.get(fIdx);
        assert (bf.lu == lu);
        if (log.isTraceEnabled()) {
            assert (bf.colIdx == rltMat.getIndex(var));
        }
        return fIdx;
    }

    private static HashMap<Pair<IloNumVar, Lu>, Integer> getBoundsFactorMap(IloLPMatrix rltMat, FactorList factors)
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
     * Returns true iff the list of integers is a consecutive list.
     */
    private static boolean areConsecutive(IntArrayList rows) {
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

    private static LongDoubleSortedVector getRltRowForLeq(Factor facJ, Factor facI, int constantVarColIdx, RltIds rltVarsInd)
            throws IloException {
        // Here we add the following constraint:
        // \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        // + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        // + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il} G_{jk})
        // w_{kl} &\leq g_ig_j

        LongDoubleSortedVector row = new LongDoubleSortedVector();
        // Part 1: \sum_{k=1}^n (g_j G_{ik} + g_i G_{jk}) x_k
        LongDoubleSortedVector facIG = new LongDoubleSortedVector(facI.G);
        LongDoubleSortedVector facJG = new LongDoubleSortedVector(facJ.G);
        facIG.scale(facJ.g);
        facJG.scale(facI.g);
        row.add(facIG);
        row.add(facJG);

        // Part 2: + \sum_{k=1}^n -G_{ik} G_{jk} w_{kk}
        LongDoubleSortedVector ip = facI.G.getProd(facJ.G);
        ip.scale(-1.0);
        LongDoubleSortedVector shiftedIp = new LongDoubleSortedVector();
        for (int idx = 0; idx < ip.getUsed(); idx++) {
            int k = SafeCast.safeLongToInt(ip.getIndices()[idx]);
            double val = ip.getValues()[idx];
            shiftedIp.set(rltVarsInd.get(k, k), val);
        }
        row.add(shiftedIp);

        // Part 3: + \sum_{k=1}^n \sum_{l=1}^{k-1} -(G_{ik} G_{jl}+ G_{il}
        // G_{jk}) w_{kl}
        for (int ii = 0; ii < facI.G.getUsed(); ii++) {
            int k = SafeCast.safeLongToInt(facI.G.getIndices()[ii]);
            double vi = facI.G.getValues()[ii];
            for (int jj = 0; jj < facJ.G.getUsed(); jj++) {
                int l = SafeCast.safeLongToInt(facJ.G.getIndices()[jj]);
                double vj = facJ.G.getValues()[jj];
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

    private static LongDoubleSortedVector getRltRowForEq(Factor facI, int k, RltIds rltVarsInd) throws IloException {
        LongDoubleSortedVector row = new LongDoubleSortedVector();

        // Original: x_k * g_i + sum_{l=1}^n - x_k * G_{il} * x_l = 0
        // Linearized: x_k * g_i + sum_{l=1}^n - G_{il} w_{kl} = 0

        // Add x_k * g_i
        row.add(k, facI.g);

        // Add sum_{l=1}^n - G_{il} w_{kl}
        for (int idx = 0; idx < facI.   G.getUsed(); idx++) {
            int l = SafeCast.safeLongToInt(facI.G.getIndices()[idx]);
            double val = - facI.G.getValues()[idx];
            row.add(rltVarsInd.get(k, l), val);
        }
        return row;
    }
    
    public FactorList getEqFactors() {
        return FactorList.unmodifiableFactorList(eqFactors);
    }

    public FactorList getLeqFactors() {
        return FactorList.unmodifiableFactorList(leqFactors);
    }

    /**
     * Gets the number of (unfiltered) RLT rows that would be produced by this set of factors.
     */
    public static long getNumRltRows(FactorList newFactors, IloLPMatrix inputMatrix) throws IloException {
        long numEqFactors = 0;
        for (FactorBuilder.Factor f : newFactors) {
            if (f.isEq()) {
                numEqFactors++;
            }
        }
        long numLeFactors = newFactors.size() - numEqFactors;
        long numRows = (numLeFactors * (numLeFactors+1)) / 2 + numEqFactors*inputMatrix.getNcols();
        return numRows;
    }
    
}
