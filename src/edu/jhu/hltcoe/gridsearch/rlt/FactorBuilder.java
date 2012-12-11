package edu.jhu.hltcoe.gridsearch.rlt;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;

import java.util.ArrayList;
import java.util.List;

import no.uib.cipr.matrix.sparse.longs.FastSparseLVector;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.SafeCast;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class FactorBuilder {

    private static final Logger log = Logger.getLogger(FactorBuilder.class);

    public abstract static class Factor {
        public double g;
        // TODO: This should really be a FastSparseVector without longs.
        public FastSparseLVector G;
        protected IloLPMatrix mat;
        
        public Factor(double g, int[] Gind, double[] Gval, IloLPMatrix mat) {
            this.g = g;
            this.G = new FastSparseLVector(SafeCast.toLong(Gind), Gval);
            this.mat = mat;
        }
        
        @Override
        public String toString() {
            return String.format("g=%f G=%s", g, G.toString());
        }
        
        public abstract boolean isEq();
        public abstract String getName() throws IloException;
    }
    
    public static enum RowFactorType {
        LOWER, EQ, UPPER
    }
    
    public static class RowFactor extends Factor {
        int rowIdx;
        RowFactorType type;
        public RowFactor(double g, int[] Gind, double[] Gval, int rowIdx, RowFactorType type, IloLPMatrix mat) {
            super(g, Gind, Gval, mat);
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
        public String getName() throws IloException {
            return mat.getRange(rowIdx).getName();
        }
    }
    
    public static class BoundFactor extends Factor {
        int colIdx;
        Lu lu;
        public BoundFactor(double g, int[] Gind, double[] Gval, int colIdx, Lu lu, IloLPMatrix mat) {
            super(g, Gind, Gval, mat);
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
        public String getName() throws IloException {
            return (lu == Lu.LOWER ? "lb:" : "ub:") + mat.getNumVar(colIdx).getName();
        }
    }

    /**
     * Creates the constraint and bounds factors.
     */
    public static List<Factor> getFactors(IloLPMatrix mat, boolean envelopeOnly)
            throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();
        
        List<Factor> factors = new ArrayList<Factor>();
        
        // Add bounds factors.
        for (int colIdx = 0; colIdx < n; colIdx++) {
            BoundFactor bfLb = getBoundFactorLower(numVars, colIdx, mat);
            if (bfLb != null) {
                factors.add(bfLb);
            }
            BoundFactor bfUb = getBoundFactorUpper(numVars, colIdx, mat);
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
    public static int addRowFactors(int startRow, int numRows, IloLPMatrix mat, List<Factor> factors)
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
                factors.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.EQ, mat));
                numNewFactors++;
            } else {
                if (!CplexUtils.isInfinite(lb[rowIdx])) {
                    // b <= A_i x
                    // 0 <= A_i x - b = (-b - (-A_i x))
                    double[] vals = Utilities.copyOf(Aval[rowIdx]);
                    Vectors.scale(vals, -1.0);
                    factors.add(new RowFactor(-lb[rowIdx], Aind[rowIdx], vals, rowIdx, RowFactorType.LOWER, mat));
                    numNewFactors++;
                }
                if (!CplexUtils.isInfinite(ub[rowIdx])) {
                    // A_i x <= b
                    // 0 <= b - A_i x
                    factors.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.UPPER, mat));
                    numNewFactors++;
                }
            }
        }
        return numNewFactors;
    }

    public static BoundFactor getBoundFactorLower(IloNumVar[] numVars, int colIdx, IloLPMatrix mat) throws IloException {
        double varLb = numVars[colIdx].getLB();
        if (!CplexUtils.isInfinite(varLb)) {
            // varLb <= x_i
            // 0 <= x_i - varLb = -varLb - (-x_i)
            int[] varInd = new int[] { colIdx };    
            double[] varVal = new double[] { -1.0 };
            return new BoundFactor(-varLb, varInd, varVal, colIdx, Lu.LOWER, mat);
        }
        return null;
    }
    
    public static BoundFactor getBoundFactorUpper(IloNumVar[] numVars, int colIdx, IloLPMatrix mat) throws IloException {
        double varUb = numVars[colIdx].getUB();
        if (!CplexUtils.isInfinite(varUb)) {
            // x_i <= varUb
            // 0 <= varUb - x_i
            int[] varInd = new int[] { colIdx };
            double[] varVal = new double[] { 1.0 };
            return new BoundFactor(varUb, varInd, varVal, colIdx, Lu.UPPER, mat);
        }
        return null;
    }

    /**
     * Gets the number of (unfiltered) rows that would be produced by this set of factors.
     */
    public static long getNumRows(List<Factor> newFactors, IloLPMatrix inputMatrix) throws IloException {
        long numEqFactors = 0;
        for (Factor f : newFactors) {
            if (f.isEq()) {
                numEqFactors++;
            }
        }
        long numLeFactors = newFactors.size() - numEqFactors;
        long numRows = (numLeFactors * (numLeFactors+1)) / 2 + numEqFactors*inputMatrix.getNcols();
        return numRows;
    }
}
