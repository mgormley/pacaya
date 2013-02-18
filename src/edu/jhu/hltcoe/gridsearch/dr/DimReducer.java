package edu.jhu.hltcoe.gridsearch.dr;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;

import java.util.Collection;

import no.uib.cipr.matrix.sparse.longs.FastSparseLVector;

import org.apache.log4j.Logger;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.tdouble.impl.SparseCCDoubleMatrix2D;
import cern.jet.random.Gamma;
import cern.jet.random.tdouble.Beta;
import edu.jhu.hltcoe.lp.CcLpConstraints;
import edu.jhu.hltcoe.lp.FactorList;
import edu.jhu.hltcoe.lp.LpRows;
import edu.jhu.hltcoe.lp.FactorBuilder.Factor;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.SafeCast;
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
        /**
         * Method of converting the input constraints (which are defined as Ax
         * <= b and Dx == d) into a set of constraints for projection.
         */
        public ConstraintConversion conversion = ConstraintConversion.EQ_TO_LEQ_PAIR;
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
    
    public enum ConstraintConversion {
        EQ_TO_LEQ_PAIR, SEPARATE_EQ_AND_LEQ
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
        if (prm.drMaxCons >= origMatrix.getNrows() && !prm.useIdentityMatrix) {
            if (prm.drMaxCons != Integer.MAX_VALUE) {
                log.warn(String.format("Parameter drMaxCons set to %d but matrix only has %d rows. " + 
                        "Skipping dimentionality reduction.", prm.drMaxCons, origMatrix.getNrows()));
            }
            // Do nothing.
            return;
        }
        
        // Add IloNumVars to lower dimensional matrix.
        int nCols = origMatrix.getNcols(); //TODO: if we add variables this needs to change.
        drMatrix.addCols(origMatrix.getNumVars());

        // Get the rows of the matrix (but not the bounds).
        FactorList factors;
        if (prm.includeBounds ) {
            factors = FactorList.getFactors(origMatrix, false);
        } else {
            // Get the rows of the matrix (but not the bounds).
            factors = FactorList.getRowFactors(origMatrix);
        }

        // Renormalize the rows.
        if (prm.renormalize) {
            factors.renormalize();
        }

        if (prm.conversion == ConstraintConversion.EQ_TO_LEQ_PAIR) {
            // Convert the original matrix to Ax <= b form.
            CcLpConstraints lc = getFactorsAsLeqConstraints(factors, nCols);
            projectAndAddConstraints(lc, drMatrix, prm.drMaxCons);
        } else if (prm.conversion == ConstraintConversion.SEPARATE_EQ_AND_LEQ) {
            // Project the equality and inequality constraints separately.
            Pair<CcLpConstraints,CcLpConstraints> pair = getFactorsAsEqAndLeqConstraints(factors, nCols);
            CcLpConstraints eqLc = pair.get1();
            CcLpConstraints leqLc = pair.get2();
            double propEq = (double) eqLc.getNumRows() / (eqLc.getNumRows() + leqLc.getNumRows());
            log.info("Proportion equality constraints: " + propEq);
            projectAndAddConstraints(eqLc, drMatrix, SafeCast.safeLongToInt(Math.round(propEq * prm.drMaxCons)));
            projectAndAddConstraints(leqLc, drMatrix, SafeCast.safeLongToInt(Math.round((1.0 - propEq) * prm.drMaxCons)));
        } else {
            throw new RuntimeException("Unhandled constraint conversion method: " + prm.conversion);
        }
        log.debug("Number of nonzeros in orig matrix: " + origMatrix.getNNZs());
        log.debug("Number of nonzeros in DR matrix: " + drMatrix.getNNZs());
    }

    /**
     * Project the constraints down to a lower dimensional space.
     * 
     * @param lc Input constraints.
     * @param drMatrix Output matrix.
     * @param drMaxCons Maximum number of constraints to form.
     */
    private void projectAndAddConstraints(CcLpConstraints lc, IloLPMatrix drMatrix, int drMaxCons)
            throws IloException {
        // Sample a random projection matrix.
        DenseDoubleMatrix2D S;
        if (prm.useIdentityMatrix) {
            log.debug("Using identity matrix for projection.");
            S = (DenseDoubleMatrix2D) DoubleFactory2D.dense.identity(lc.A.rows());
        } else {
            S = sampleMatrix(drMaxCons, lc.A.rows());
        }
        log.debug("Number of nonzeros in S matrix: " + getNumNonZeros(S));        
        
        // Multiply the random projection with A and b, d.
        DenseDoubleMatrix2D Sd = fastMultiply(S, lc.d);
        DenseDoubleMatrix2D SA = fastMultiply(S, lc.A);
        DenseDoubleMatrix2D Sb = fastMultiply(S, lc.b);

        // Construct the lower-dimensional constraints: Sd <= SA x <= Sb.
        LpRows rows = new LpRows(prm.setNames);
        for (int i=0; i<SA.rows(); i++) {
            FastSparseLVector coef = new FastSparseLVector();
            for (int j=0; j<SA.columns(); j++) {
                double SA_ij = SA.getQuick(i, j);
                if (!Utilities.equals(SA_ij, 0.0, prm.multZeroDelta)) {
                    coef.set(j, SA_ij);
                }
            }
            double lb = Sd.getQuick(i,0);
            double ub = Sb.getQuick(i,0);
            String name = String.format("dr_%d", i);
            rows.addRow(lb, coef, ub, name);
        }
        rows.addRowsToMatrix(drMatrix);
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
     * Get a new matrix representing the original constraints d <= Ax <= b as Gx
     * <= g by converting each equality constraint, A_i x == b_i, to a pair of
     * inequalities, A_i x <= b_i and A_i x >= b_i.
     * 
     * @param factors, int nCols
     *            Input matrix.
     * @return A new matrix representing Gx <= g
     * @throws IloException
     */
    private static CcLpConstraints getFactorsAsLeqConstraints(FactorList factors, int nCols) throws IloException {        
        // Convert each EQ factor into a pair of LEQ constraints.
        factors = FactorList.convertToLeqFactors(factors);
        
        // Convert to a compressed column store of the Ax <= b constraints.
        return CcLpConstraints.getLeqFactorsAsLeqConstraints(factors, nCols);
    }

    /**
     * Get a new pair of constraint sets representing the original constraints 
     * d <= Ax <= b as Gx == g, and Hx <= h.
     * 
     * @param origMatrix Input matrix.
     * @return Pair of constraint sets. The first is Gx == g. The second is Hx <= h.
     */
    private static Pair<CcLpConstraints, CcLpConstraints> getFactorsAsEqAndLeqConstraints(FactorList factors, int nCols) {
        FactorList eqFactors = new FactorList();
        FactorList leqFactors = new FactorList();
        for (Factor factor : factors) {
                if (factor.isEq()) {
                    eqFactors.add(factor);
                } else {
                    leqFactors.add(factor);
                }
        }

        // Convert to a compressed column store of the Gx == g constraints.
        CcLpConstraints eqLc = CcLpConstraints.getEqFactorsAsEqConstraints(eqFactors, nCols);
        
        // Convert to a compressed column store of the Hx <= h constraints.
        CcLpConstraints leqLc = CcLpConstraints.getLeqFactorsAsLeqConstraints(leqFactors, nCols);
        
        return new Pair<CcLpConstraints, CcLpConstraints>(eqLc, leqLc);
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
        boolean allInfinite = true;
        boolean someInfinite = false;
        for (int i=0; i<b.rows(); i++) {
            if (CplexUtils.isInfinite(b.getQuick(i, 0))) {
                someInfinite = true;
            } else {
                allInfinite = false;
            }
        }
        
        if (allInfinite) {
            return b;
        }
        if (someInfinite && !allInfinite) {
            log.error("Mixture of EQ and LEQ constraints in a projection will result in NaNs.");
            throw new RuntimeException("Mixture of EQ and LEQ constraints in a projection will result in NaNs.");
        }
        
        DenseDoubleMatrix2D Sb = new DenseDoubleMatrix2D(S.rows(), b.columns());
        S.zMult(b, Sb);
        return Sb;
    }

    // Allow this to be overriden in unit tests.
    protected DenseDoubleMatrix2D sampleMatrix(int nRows, int nCols) throws IloException {
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
