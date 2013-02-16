package edu.jhu.hltcoe.gridsearch.dr;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloRange;

import java.util.ArrayList;
import java.util.List;

import no.uib.cipr.matrix.sparse.longs.FastSparseLVector;
import no.uib.cipr.matrix.sparse.longs.LVectorEntry;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder;
import edu.jhu.hltcoe.gridsearch.rlt.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.SafeCast;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cplex.CplexRows;
import edu.jhu.hltcoe.util.cplex.CplexUtils;

/**
 * Reduces the dimensionality of a linear program.
 * 
 * @author mgormley
 * 
 */
public class DimReducer {
    
    public static class DimReducerPrm {
        public int drMaxCons = 10; //Integer.MAX_VALUE;
        public boolean setNames = true;
        public double zeroCutoff = 1e-8;
        public boolean renormalize = true;
    }
    
    /**
     * Represents constraints Ax <= b
     */
    private static class LeqConstraints {
        SparseCCDoubleMatrix2D A;
        DenseDoubleMatrix2D b;
        public LeqConstraints(SparseCCDoubleMatrix2D A, DenseDoubleMatrix2D b) {
            this.A = A;
            this.b = b;
        }        
    }
    
    private static final Logger log = Logger.getLogger(DimReducer.class);

    private DimReducerPrm prm;

    public DimReducer(DimReducerPrm prm) {
        this.prm = prm;
    }

    /**
     * Reduces the dimensionality of a matrix representing a set of linear
     * constraints.
     * 
     * @param origMatrix
     *            Input matrix.
     * @param drMatrix
     *            Output matrix.
     * @throws IloException
     */
    public void reduceDimensionality(IloLPMatrix origMatrix, IloLPMatrix drMatrix) throws IloException {
        if (prm.drMaxCons >= origMatrix.getNrows()) {
            // Do nothing.
            return;
        }

        // Convert the original matrix to Ax <= b form.
        LeqConstraints lc = getMatrixAsLeqConstraints(origMatrix, prm.renormalize);

        // Sample a random projection matrix.
        DenseDoubleMatrix2D S = sampleMatrix(prm.drMaxCons, lc.A.rows());
                
        // Multiply the random projection with A and b.
        DenseDoubleMatrix2D SA = fastMultiply(S, lc.A);
        DenseDoubleMatrix2D Sb = fastMultiply(S, lc.b);
        
        // Construct the lower-dimensional constraints: SA x <= Sb.
        drMatrix.addCols(origMatrix.getNumVars());

        // Loop through the rows of SA <= Sb and build the constraints' LHS.
        CplexRows rows = new CplexRows(prm.setNames);
        for (int i=0; i<SA.rows(); i++) {
            FastSparseLVector coef = new FastSparseLVector();
            for (int j=0; j<SA.columns(); j++) {
                double SA_ij = SA.getQuick(i, j);
                if (!Utilities.equals(SA_ij, 0.0, prm.zeroCutoff)) {
                    coef.set(j, SA_ij);
                }
            }
            double ub = Sb.getQuick(i,0);
            String name = String.format("dr_%d", i);
            rows.addRow(CplexUtils.CPLEX_NEG_INF, coef, ub, name);
        }
        rows.addRowsToMatrix(drMatrix);
    }

    public static DenseDoubleMatrix2D fastMultiply(DenseDoubleMatrix2D S, SparseCCDoubleMatrix2D A) {
        // Doing this: S.zMult(A, SA); would ignore the sparsity of the matrix A.
        // Instead, we call zMult and transpose all the matrices.
        // (A B)^T = A^T B^T
        DenseDoubleMatrix2D SA = new DenseDoubleMatrix2D(S.rows(), A.columns());
        A.zMult(S, SA.viewDice(), 1.0, 0, true, true);
        return SA;
    }
    
    public static DenseDoubleMatrix2D fastMultiply(DenseDoubleMatrix2D S, DenseDoubleMatrix2D b) {
        DenseDoubleMatrix2D Sb = new DenseDoubleMatrix2D(S.rows(), b.columns());
        S.zMult(b, Sb);
        return Sb;
    }

    // Allow this to be overriden in unit tests.
    protected DenseDoubleMatrix2D sampleMatrix(int nRows, int nCols) {
        DenseDoubleMatrix2D S = new DenseDoubleMatrix2D(nRows, nCols);
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                S.set(i, j, Prng.nextDouble());
            }
        }
        return S;
    }

    /**
     * Get a new matrix representing the original constraints d <= Ax <= b as
     * Gx <= g.
     * 
     * @param origMatrix Input matrix.
     * @return A new matrix representing Gx <= g
     * @throws IloException 
     */
    private static LeqConstraints getMatrixAsLeqConstraints(IloLPMatrix origMatrix, boolean renormalize) throws IloException {
        List<Factor> factors = FactorBuilder.getRowFactors(origMatrix);

        // Convert each EQ factor into a pair of LEQ constraints.
        factors = convertToLeqFactors(factors);
        
        if (renormalize) {
            // Compute the average L2 norm for this matrix.
            double avgL2Norm = 0.0;
            for (Factor fac : factors) {
                avgL2Norm += Math.sqrt(getSumOfSquares(fac));
            }
            avgL2Norm /= factors.size();
            
            // Renormalize the factors, so that each one has an L2 norm of 
            // the average L2 norm.
            for (Factor fac : factors) {
                double ss = getSumOfSquares(fac);
                fac.g = avgL2Norm * fac.g / Math.sqrt(ss);
                double[] vals = fac.G.getData();
                for (int i=0; i<fac.G.getUsed(); i++) {
                    vals[i] = avgL2Norm * vals[i] / Math.sqrt(ss);
                }
            }
        }

        // Count the number of non zeros in the factors.
        int numNonZerosInG = getNumNonZerosInG(factors);

        // Set the number of rows and columns.
        int nRows = factors.size();
        int nCols = origMatrix.getNcols(); //TODO: if we add variables this needs to change.
        
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
        
        return new LeqConstraints(G, g);
    }

    /**
     * Converts each EQ factor into a pair of LEQ constraints.
     */
    private static List<Factor> convertToLeqFactors(List<Factor> factors) {
        // Convert each EQ factor into a pair of LEQ constraints.
        List<Factor> leqFactors = new ArrayList<Factor>();
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

    /**
     * Counts the number of non zeros in the factors G vectors.
     */
    private static int getNumNonZerosInG(List<Factor> factors) {
        int numNonZeros = 0;
        for (Factor factor : factors) {
            // TODO: decide whether to check for literal zeros.
            // Do not count factor.g as one. Just the nonzeros in factor.G.
            numNonZeros += factor.G.getUsed();
        }
        return numNonZeros;
    }
    
    /**
     * Gets the sum of squares of the coefficients of the factor:
     * 
     * @param fac Input factor.
     * @return The value g^2 + \sum_{i} G_i^2
     */
    private static double getSumOfSquares(Factor fac) {
        double ss = fac.g * fac.g;
        for (LVectorEntry ve : fac.G) {
            ss += ve.get() * ve.get();
        }
        return ss;
    }

    /** 
     * Converts a CPLEX matrix to a sparse matrix representation.
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
