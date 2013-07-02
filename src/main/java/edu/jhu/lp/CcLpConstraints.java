package edu.jhu.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import edu.jhu.util.vector.LongDoubleEntry;

import org.apache.log4j.Logger;

import edu.jhu.util.matrix.DenseDoubleMatrix;
import edu.jhu.util.matrix.SparseColDoubleMatrix;
import edu.jhu.lp.FactorBuilder.Factor;
import edu.jhu.util.SafeCast;

/**
 * Represents constraints d <= Ax <= b, where A is a matrix in compressed-column form and
 * is amenable to pre multiplication, and where d and b are vectors.
 */
public class CcLpConstraints {
    private static final Logger log = Logger.getLogger(CcLpConstraints.class);

    public DenseDoubleMatrix d;
    public SparseColDoubleMatrix A;
    public DenseDoubleMatrix b;

    /**
     * Constructs a set of constraints representing d <= A <= b.
     */
    private CcLpConstraints(DenseDoubleMatrix d, SparseColDoubleMatrix A, DenseDoubleMatrix b) {
        this.d = d;
        this.A = A;
        this.b = b;
    }

    public int getNumRows() {
        return A.getNumRows();
    }
    
    public int getNumCols() {
        return A.getNumColumns();
    }
    
    /**
     * Factory method: Gets a compressed column representation of the factors.
     * @param factors Input EQ factors.
     * @param nCols Number of columns in the input factors (deprecated).
     * @throws IllegalStateException If any of the factors are EQ factors. 
     * @return The new Gx <= g constraints.
     */
    public static CcLpConstraints getLeqFactorsAsLeqConstraints(FactorList factors, int nCols) {
        for (Factor f : factors) {
            if (f.isEq()) {
                throw new IllegalStateException("All factors must be inequalities");
            }
        }
        return getFactorsAsCcLpConstraints(factors, nCols);
    }
    
    /**
     * Factory method: Gets a compressed column representation of the factors.
     * @param factors Input EQ factors.
     * @param nCols Number of columns in the input factors (deprecated).
     * @throws IllegalStateException If any of the factors are EQ factors. 
     * @return The new Gx <= g constraints.
     */
    public static CcLpConstraints getEqFactorsAsEqConstraints(FactorList factors, int nCols) {
        for (Factor f : factors) {
            if (!f.isEq()) {
                throw new IllegalStateException("All factors must be equalities");
            }
        }        
        return getFactorsAsCcLpConstraints(factors, nCols);
    }

    private static CcLpConstraints getFactorsAsCcLpConstraints(FactorList factors, int nCols) {
        // Count the number of non zeros in the factors.
        int numNonZerosInG = getNumNonZerosInG(factors);

        // Set the number of rows and columns.
        int nRows = factors.size();
        
        // Copy factors into sparse matrix representation: for A.
        int[] rowIndexes = new int[numNonZerosInG];
        int[] colIndexes = new int[numNonZerosInG];
        double[] values = new double[numNonZerosInG];

        int count = 0;
        for (int i=0; i<factors.size(); i++) {
            for (LongDoubleEntry ve : factors.get(i).G) {
                rowIndexes[count] = i;
                colIndexes[count] = SafeCast.safeLongToInt(ve.index());
                values[count] = ve.get();
                count++;
            }
        }

        // Construct sparse matrix: A.
        log.debug("Constructing G matrix");
        SparseColDoubleMatrix A = new SparseColDoubleMatrix(nRows, nCols, rowIndexes, colIndexes, values);
        
        // Construct constraint bounds: b.
        log.debug("Constructing g vector");
        DenseDoubleMatrix d = new DenseDoubleMatrix(nRows, 1);
        DenseDoubleMatrix b = new DenseDoubleMatrix(nRows, 1);
        for (int i=0; i<factors.size(); i++) {
            Factor factor = factors.get(i);
            if (factor.isEq()) {
                d.set(i, 0, factor.g);
            } else {
                d.set(i, 0, Double.NEGATIVE_INFINITY);
            }
            b.set(i, 0, factor.g);
        }
        
        return new CcLpConstraints(d, A, b);
    }
    
    /**
     * Counts the number of non zeros in the factors G vectors.
     */
    private static int getNumNonZerosInG(FactorList factors) {
        int numNonZeros = 0;
        for (Factor factor : factors) {
            // TODO: decide whether to check for literal zeros.
            // Do not count factor.g as one. Just the nonzeros in factor.G.
            numNonZeros += factor.G.getUsed();
        }
        return numNonZeros;
    }

    /** 
     * Converts a CPLEX matrix to a compressed-column sparse matrix representation.
     */
    @Deprecated
    public static SparseColDoubleMatrix getAsSparseMatrix(IloLPMatrix origMatrix) throws IloException {
        // Get columns from CPLEX.
        int numNonZeros = origMatrix.getNNZs();
        int nRows = origMatrix.getNrows();
        int nCols = origMatrix.getNcols();
        int[][] cpxRowInds = new int[nCols][];
        double[][] cpxVals = new double[nCols][];

        origMatrix.getCols(0, nCols, cpxRowInds, cpxVals);

        // Copy columns into sparse matrix representation.
        int[] rowIndexes = new int[numNonZeros];
        int[] colIndexes = new int[numNonZeros];
        double[] values = new double[numNonZeros];

        int count = 0;
        for (int col = 0; col < nCols; col++) {
            for (int i = 0; i < cpxRowInds.length; i++) {
                rowIndexes[count] = cpxRowInds[col][i];
                colIndexes[count] = col;
                values[count] = cpxVals[col][i];
                count++;
            }
        }

        // Construct sparse matrix.
        SparseColDoubleMatrix A = new SparseColDoubleMatrix(nRows, nCols, rowIndexes, colIndexes, values);
        return A;
    }

}