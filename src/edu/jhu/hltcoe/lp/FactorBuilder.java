package edu.jhu.hltcoe.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import edu.jhu.hltcoe.util.vector.SortedLongDoubleVector;
import edu.jhu.hltcoe.util.vector.LongDoubleEntry;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Lu;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.SafeCast;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

public class FactorBuilder {

    private static final Logger log = Logger.getLogger(FactorBuilder.class);

    /**
     * Represents a constraint: as either (g - Gx) >= 0 or (g - Gx) == 0.
     */
    public abstract static class Factor {
        public double g;
        // TODO: This should really be a FastSparseVector without longs.
        public SortedLongDoubleVector G;
        protected IloLPMatrix mat;
        
        public Factor(double g, int[] Gind, double[] Gval, IloLPMatrix mat) {
            this.g = g;
            this.G = new SortedLongDoubleVector(SafeCast.toLong(Gind), Gval);
            this.mat = mat;
        }
        
        public Factor(double g, SortedLongDoubleVector G, IloLPMatrix mat) {
            this.g = g;
            this.G = G;
            this.mat = mat;
        }
        
        @Override
        public String toString() {
            return String.format("g=%f G=%s", g, G.toString());
        }
        
        public abstract boolean isEq();
        public abstract String getName() throws IloException;

        /** 
         * Gets the L2 norm of the coefficients of the factor, including the bound.
         * 
         * @param fac Input factor.
         * @return The value sqrt(g^2 + \sum_{i} G_i^2).
         */
        public double getL2Norm() {
            return Math.sqrt(getSumOfSquares());
        }
        
        /**
         * Gets the sum of squares of the coefficients of the factor, including the bound.
         * 
         * @param fac Input factor.
         * @return The value (g^2 + \sum_{i} G_i^2).
         */
        private double getSumOfSquares() {
            double ss = g * g;
            for (LongDoubleEntry ve : G) {
                ss += ve.get() * ve.get();
            }
            return ss;
        }
    }
    
    public static enum RowFactorType {
        LOWER, EQ, UPPER
    }
    
    public static class RowFactor extends Factor {
        public int rowIdx;
        RowFactorType type;
        public RowFactor(double g, int[] Gind, double[] Gval, int rowIdx, RowFactorType type, IloLPMatrix mat) {
            super(g, Gind, Gval, mat);
            this.rowIdx = rowIdx;
            this.type = type;
        }
        public RowFactor(double g, SortedLongDoubleVector G, int rowIdx, RowFactorType type, IloLPMatrix mat) {
            super(g, G, mat);
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
        public int colIdx;
        public Lu lu;
        public BoundFactor(double g, int[] Gind, double[] Gval, int colIdx, Lu lu, IloLPMatrix mat) {
            super(g, Gind, Gval, mat);
            this.colIdx = colIdx;
            this.lu = lu;
        }
        public BoundFactor(double g, SortedLongDoubleVector G, int colIdx, Lu lu, IloLPMatrix mat) {
            super(g, G, mat);
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
     * Convert EQ factor into a pair of LEQ factors.
     * A_i^T x == b_i becomes A_i^T x >= b_i and A_i^T x <= b_i.
     * @param eq Equality constraint as a factor.
     * @return Pair of inequality constraints equivalent to input factor.
     */
    public static Pair<Factor, Factor> getEqFactorAsLeqPair(Factor eq) {
        if (!eq.isEq()) {
            throw new IllegalStateException("Input factor must be an equality");
        }

        Factor leq1;
        Factor leq2;
        SortedLongDoubleVector negG = new SortedLongDoubleVector(eq.G);
        negG.scale(-1.0);
        if (eq instanceof RowFactor) {
            RowFactor rf = (RowFactor)eq;
            // (g - Gx) >= 0 ==> Gx <= g
            leq1 = new RowFactor(rf.g, new SortedLongDoubleVector(rf.G), rf.rowIdx, RowFactorType.UPPER, rf.mat);
            // (g - Gx) <= 0 ==> g <= Gx ==> (-g + Gx) >= 0 
            leq2 = new RowFactor(-rf.g, negG, rf.rowIdx, RowFactorType.LOWER, rf.mat);
        } else if (eq instanceof BoundFactor) {
            throw new IllegalStateException("Bounds factors are currently never equality constraints.");
        } else {
            throw new IllegalStateException("not implemented");
        }
        
        return new Pair<Factor,Factor>(leq1, leq2);
    }
}
