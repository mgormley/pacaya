package edu.jhu.hltcoe.gridsearch.dr;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;

import java.util.Collection;

import no.uib.cipr.matrix.sparse.longs.FastSparseLVector;

import org.apache.log4j.Logger;

import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import cern.jet.random.Gamma;
import cern.jet.random.tdouble.Beta;
import edu.jhu.hltcoe.lp.CcLeqConstraints;
import edu.jhu.hltcoe.lp.FactorList;
import edu.jhu.hltcoe.lp.LpRows;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cplex.CplexUtils;
import edu.jhu.hltcoe.util.tuple.OrderedPair;
import edu.jhu.hltcoe.util.tuple.PairSampler;

/**
 * Reduces the dimensionality of a linear program.
 * 
 * @author mgormley
 * 
 */
public class DimReducer {
    
    public static class DimReducerPrm {
        public int drMaxCons = Integer.MAX_VALUE;
        public boolean setNames = true;
        public boolean renormalize = true;
        public boolean includeBounds = false;
        /**
         * The delta for checking zero in the sampled matrix.
         */
        public double samplingZeroCutoff = 1e-2;
        public SamplingDistribution dist = SamplingDistribution.UNIFORM;
        /**
         * Parameter for Beta and Gamma distribution.
         */
        public double alpha = 0.01;
        /**
         * Parameter for Beta and Gamma distribution.
         * For Gamma, this is the rate parameter beta = 1 / lambda. 
         */
        public double beta = 100;
        /**
         * The delta for deciding what a zero is in the projected matrix, SA.
         * Care should be taken when changing this since it could produce an infeasibility.
         */
        public double multZeroDelta = 1e-13;
        public int maxNonZeros = Integer.MAX_VALUE;
        /**
         * This parameter uses the identity matrix for S, overriding all other
         * projection preferences. This is just used as a sanity check.
         */
        public boolean useIdentityMatrix = false;
    }

    /**
     * Enumeration of sampling distributions for populating the projection
     * matrix.
     * 
     * Note that these all sample from the positive reals, since a negative
     * number would flip the inequality.
     * 
     * TODO: If we switch to all equality constraints (Ax = b), we could use
     * sampling distributions with negative numbers.
     */
    public enum SamplingDistribution {
        UNIFORM, BETA, GAMMA, ALL_ONES
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
            if (prm.drMaxCons != Integer.MAX_VALUE) {
                log.warn(String.format("Parameter drMaxCons set to %d but matrix only has %d rows. " + 
                        "Skipping dimentionality reduction.", prm.drMaxCons, origMatrix.getNrows()));
            }
            // Do nothing.
            return;
        }
        
        // Convert the original matrix to Ax <= b form.
        CcLeqConstraints lc = getMatrixAsLeqConstraints(origMatrix);

        // Sample a random projection matrix.
        DenseDoubleMatrix2D S = sampleMatrix(prm.drMaxCons, lc.A.rows(), origMatrix);
        log.debug("Number of nonzeros in S matrix: " + getNumNonZeros(S));        
        
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
                if (!Utilities.equals(SA_ij, 0.0, prm.multZeroDelta)) {
                    coef.set(j, SA_ij);
                }
            }
            double ub = Sb.getQuick(i,0);
            String name = String.format("dr_%d", i);
            rows.addRow(CplexUtils.CPLEX_NEG_INF, coef, ub, name);
        }
        rows.addRowsToMatrix(drMatrix);        
        log.debug("Number of nonzeros in orig matrix: " + origMatrix.getNNZs());
        log.debug("Number of nonzeros in DR matrix: " + drMatrix.getNNZs());
    }

    private int getNumNonZeros(DenseDoubleMatrix2D S) {
        int count = 0;
        for (int i=0; i<S.rows(); i++) {
            for (int j=0; j<S.columns(); j++) {
                double SA_ij = S.getQuick(i, j);
                if (!Utilities.equals(SA_ij, 0.0, prm.multZeroDelta)) {
                    count++;
                }
            }
        }
        return count;
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
        return CcLeqConstraints.getLeqFactorsAsLeqConstraints(factors, nCols);
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
    protected DenseDoubleMatrix2D sampleMatrix(int nRows, int nCols, IloLPMatrix origMatrix) throws IloException {
        DenseDoubleMatrix2D S = new DenseDoubleMatrix2D(nRows, nCols);
        
        // Sample a subset of the matrix elements for which to sample a (potentially) nonzero value.
        double prop = Math.min(1.0, (double) prm.maxNonZeros  / (nRows * nCols));
        log.debug("Proportion of nonzeros in S matrix: " + prop);
        Collection<OrderedPair> pairs = PairSampler.sampleOrderedPairs(0, nRows, 0, nCols, prop);
        
        Beta betaDist = new Beta(prm.alpha, prm.beta, Prng.doubleMtColt);
        Gamma gammaDist = new Gamma(prm.alpha, 1.0/prm.beta, Prng.mtColt);
        for (OrderedPair pair : pairs) {
            // Sample the value for this (potential) nonzero.
            double val;
            if (prm.dist == SamplingDistribution.UNIFORM) {
                val = Prng.nextDouble();
            } else if (prm.dist == SamplingDistribution.BETA) {
                val = betaDist.nextDouble();
            } else if (prm.dist == SamplingDistribution.GAMMA) {
                val = gammaDist.nextDouble();
            } else if (prm.dist == SamplingDistribution.ALL_ONES) {
                val = 1;
            } else {
                throw new IllegalStateException("Unhandled sampling distribution: " + prm.dist);
            }
            
            if (!Utilities.equals(val, 0.0, prm.samplingZeroCutoff)) {
                // Set the non-zero value in the matrix.
                S.set(pair.get1(), pair.get2(), val);
            }
        }
        
        return S;
    }


}
