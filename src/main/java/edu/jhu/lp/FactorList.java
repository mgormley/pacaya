package edu.jhu.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.lp.FactorBuilder.RowFactor;
import edu.jhu.lp.FactorBuilder.RowFactorType;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.Pair;
import edu.jhu.util.Utilities;
import edu.jhu.util.cplex.CplexUtils;

/**
 * Represents a set of constraints or variable bounds as Gx <= g, or
 * equivalently as (g - Gx) >= 0. These constraints are stored in a sparse
 * row-compressed representation.
 * 
 * @author mgormley
 * 
 */
public class FactorList implements Iterable<Factor> {
    
    private static final Logger log = Logger.getLogger(FactorBuilder.class);
    private static final long serialVersionUID = 7998911214212040462L;
    protected ArrayList<Factor> rows;

    public FactorList() {
        this.rows = new ArrayList<Factor>();
    }
    
    public FactorList(ArrayList<Factor> rows) {
        this.rows = rows;
    }

    public void add(Factor factor) {
        rows.add(factor);
    }
    
    public int size() {
        return rows.size();
    }

    public Factor get(int i) {
        return rows.get(i);
    }

    @Override
    public Iterator<Factor> iterator() {
        return rows.iterator();
    }

    public void set(int index, Factor factor) {
        rows.set(index, factor);
    }

    public FactorList sublist(int start, int end) {
        return new FactorList(Utilities.sublist(rows, start, end));
    }

    /**
     * Factory method: Gets only the constraints of the matrix as factors.
     */
    public static FactorList getRowFactors(IloLPMatrix mat) throws IloException {
        FactorList factors = new FactorList();
        int startRow = 0;
        int numRows = mat.getNrows();
        factors.addRowFactors(startRow, numRows, mat);
        return factors;
    }

    /**
     * Factory method: Gets only the constraints of the matrix and
     * the bounds of the variables as factors.
     */
    public static FactorList getFactors(IloLPMatrix mat, boolean boundFactorsOnly)
            throws IloException {
        int n = mat.getNcols();
        int m = mat.getNrows();
        IloNumVar[] numVars = mat.getNumVars();
        
        FactorList factors = new FactorList();
        
        // Add bounds factors.
        for (int colIdx = 0; colIdx < n; colIdx++) {
            FactorBuilder.BoundFactor bfLb = FactorBuilder.getBoundFactorLower(numVars, colIdx, mat);
            if (bfLb != null) {
                factors.add(bfLb);
            }
            FactorBuilder.BoundFactor bfUb = FactorBuilder.getBoundFactorUpper(numVars, colIdx, mat);
            if (bfUb != null) {
                factors.add(bfUb);
            }
        }
    
        if (!boundFactorsOnly) {
            // Add constraint factors.
            int startRow = 0;
            int numRows = m;            
            factors.addRowFactors(startRow, numRows, mat);
        }
        
        if (log.isTraceEnabled()) {
            log.trace("factors: ");
            for (FactorBuilder.Factor f : factors) {
                log.trace("\t" + f);
            }
        }
        return factors;
    }

    /**
     * Create numRows RowFactors starting from the startRow'th row of mat and add these rows to factors.
     * @return The number of new factors added to factors.
     */
    public int addRowFactors(int startRow, int numRows, IloLPMatrix mat)
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
                this.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.EQ, mat));
                numNewFactors++;
            } else {
                if (!CplexUtils.isInfinite(lb[rowIdx])) {
                    // b <= A_i x
                    // 0 <= A_i x - b = (-b - (-A_i x))
                    double[] vals = DoubleArrays.copyOf(Aval[rowIdx]);
                    DoubleArrays.scale(vals, -1.0);
                    this.add(new RowFactor(-lb[rowIdx], Aind[rowIdx], vals, rowIdx, RowFactorType.LOWER, mat));
                    numNewFactors++;
                }
                if (!CplexUtils.isInfinite(ub[rowIdx])) {
                    // A_i x <= b
                    // 0 <= b - A_i x
                    this.add(new RowFactor(ub[rowIdx], Aind[rowIdx], Aval[rowIdx], rowIdx, RowFactorType.UPPER, mat));
                    numNewFactors++;
                }
            }
        }
        return numNewFactors;
    }


    /**
     * Renormalize the factors in place, so that each one has an L2 norm of 
     * the average L2 norm.
     */
    public void renormalize() {
        // Compute the average L2 norm for this matrix.
        double avgL2Norm = 0.0;
        for (Factor fac : this) {
            avgL2Norm += fac.getL2Norm();
        }
        avgL2Norm /= this.size();
        
        // Renormalize the factors, so that each one has an L2 norm of 
        // the average L2 norm.
        for (Factor fac : this) {
            double l2Norm = fac.getL2Norm();
            fac.g = avgL2Norm * fac.g / l2Norm;
            double[] vals = fac.G.getValues();
            for (int i=0; i<fac.G.getUsed(); i++) {
                vals[i] = avgL2Norm * vals[i] / l2Norm;
            }
        }
    }
    
    /**
     * Converts each EQ factor into a pair of LEQ constraints.
     */
    public static FactorList convertToLeqFactors(FactorList factors) {
        // Convert each EQ factor into a pair of LEQ constraints.
        FactorList leqFactors = new FactorList();
        for (Factor factor : factors) {
            if (factor.isEq()) {
                Pair<Factor, Factor> pair = FactorBuilder.getEqFactorAsLeqPair(factor);
                leqFactors.add(pair.get1());
                leqFactors.add(pair.get2());
            } else {
                leqFactors.add(factor);
            }
        }
    
        // Use this new set of only LEQ factors.
        return leqFactors;
    }

    @Override
    public String toString() {
        return "FactorList [rows=" + rows + "]";
    }

    /**
     * Gets an immutable representation of the factors.
     */
    public static FactorList unmodifiableFactorList(FactorList factors) {
        return new ImmutableFactorList(factors);
    }
    
    /**
     * An immutable version of the FactorList.
     */
    private static class ImmutableFactorList extends FactorList {

        public ImmutableFactorList(FactorList eqFactors) {
            this.rows = eqFactors.rows;
        }

        @Override
        public void add(Factor factor) {
            throw new RuntimeException("Cannot modify an ImmutableFactorList");
        }

        @Override
        public Iterator<Factor> iterator() {
            return Collections.unmodifiableList(rows).iterator();
        }

        @Override
        public void set(int index, Factor factor) {
            throw new RuntimeException("Cannot modify an ImmutableFactorList");
        }
        
    }
}