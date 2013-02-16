package edu.jhu.hltcoe.gridsearch.dr;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import no.uib.cipr.matrix.sparse.longs.FastSparseLVector;
import no.uib.cipr.matrix.sparse.longs.LVectorEntry;

import org.apache.log4j.Logger;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import edu.jhu.hltcoe.lp.CcLeqConstraints;
import edu.jhu.hltcoe.lp.FactorList;
import edu.jhu.hltcoe.lp.LpRows;
import edu.jhu.hltcoe.lp.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
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
        public boolean includeBounds = false;
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
        CcLeqConstraints lc = getMatrixAsLeqConstraints(origMatrix);

        // Sample a random projection matrix.
        DenseDoubleMatrix2D S = sampleMatrix(prm.drMaxCons, lc.A.rows());
                
        // Multiply the random projection with A and b.
        DenseDoubleMatrix2D SA = fastMultiply(S, lc.A);
        DenseDoubleMatrix2D Sb = fastMultiply(S, lc.b);
        
        // Construct the lower-dimensional constraints: SA x <= Sb.
        drMatrix.addCols(origMatrix.getNumVars());

        // Loop through the rows of SA <= Sb and build the constraints' LHS.
        LpRows rows = new LpRows(prm.setNames);
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

    /**
     * Get a new matrix representing the original constraints d <= Ax <= b as
     * Gx <= g.
     * 
     * @param origMatrix Input matrix.
     * @return A new matrix representing Gx <= g
     * @throws IloException 
     */
    public CcLeqConstraints getMatrixAsLeqConstraints(IloLPMatrix origMatrix) throws IloException {
        // Get the rows of the matrix (but not the bounds).
        FactorList factors;
        if (prm.includeBounds ) {
            factors = FactorList.getFactors(origMatrix, false);
        } else {
            // Get the rows of the matrix (but not the bounds).
            factors = FactorList.getRowFactors(origMatrix);
        }

        // Convert each EQ factor into a pair of LEQ constraints.
        factors = FactorList.convertToLeqFactors(factors);
        
        // Renormalize the rows.
        if (prm.renormalize) {
            factors.renormalize();
        }

        // Convert to a compressed column store of the Ax <= b constraints.
        int nCols = origMatrix.getNcols(); //TODO: if we add variables this needs to change.
        return CcLeqConstraints.getFactorsAsLeqConstraints(factors, nCols);
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


}
