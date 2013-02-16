package edu.jhu.hltcoe.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import no.uib.cipr.matrix.sparse.longs.LVectorEntry;

import org.apache.log4j.Logger;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import edu.jhu.hltcoe.lp.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.SafeCast;

/**
 * Represents constraints Ax <= b, where A is in compressed-column form and
 * is amenable to pre multiplication.
 */
public class CcLeqConstraints {
    private static final Logger log = Logger.getLogger(CcLeqConstraints.class);

    public SparseCCDoubleMatrix2D A;
    public DenseDoubleMatrix2D b;
    
    private CcLeqConstraints(SparseCCDoubleMatrix2D A, DenseDoubleMatrix2D b) {
        this.A = A;
        this.b = b;
    }

    /**
     * Factory method: Gets a compressed column representation of the factors.
     * @param factors Input factors.
     * @param nCols Number of columns in the input factors (deprecated). 
     * @return The new Gx <= g constraints.
     */
    public static CcLeqConstraints getFactorsAsLeqConstraints(FactorList factors, int nCols) {
        // Count the number of non zeros in the factors.
        int numNonZerosInG = getNumNonZerosInG(factors);

        // Set the number of rows and columns.
        int nRows = factors.size();
        
        // Copy factors into sparse matrix representation: for G.
        int[] rowIndexes = new int[numNonZerosInG];
        int[] colIndexes = new int[numNonZerosInG];
        double[] values = new double[numNonZerosInG];

        int count = 0;
        for (int i=0; i<factors.size(); i++) {
            for (LVectorEntry ve : factors.get(i).G) {
                rowIndexes[count] = i;
                colIndexes[count] = SafeCast.safeToInt(ve.index());
                values[count] = ve.get();
                count++;
            }
        }

        // Construct sparse matrix: G.
        log.debug("Constructing G matrix");
        boolean sortRowIndices = true;
        SparseCCDoubleMatrix2D G = new SparseCCDoubleMatrix2D(nRows, nCols, rowIndexes, colIndexes, values, false, false, sortRowIndices);
        
        // Construct constraint bounds: g.
        log.debug("Constructing g vector");
        DenseDoubleMatrix2D g = new DenseDoubleMatrix2D(nRows, 1);
        for (int i=0; i<factors.size(); i++) {
            g.setQuick(i, 0, factors.get(i).g);
        }
        
        return new CcLeqConstraints(G, g);
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
    public static SparseCCDoubleMatrix2D getAsSparseMatrix(IloLPMatrix origMatrix) throws IloException {
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
        SparseCCDoubleMatrix2D A = new SparseCCDoubleMatrix2D(nRows, nCols, rowIndexes, colIndexes, values, false,
                false, false);
        return A;
    }

}