package edu.jhu.util.cplex;

import static org.junit.Assert.assertTrue;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BasisStatus;

import java.util.Arrays;

import org.junit.Assert;

import edu.jhu.gridsearch.rlt.SymmetricMatrix.SymVarMat;
import edu.jhu.prim.util.sort.IntDoubleSort;
import edu.jhu.prim.vector.IntDoubleSortedVector;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

public class CplexUtils {

    /**
     * The CPLEX representation of positive infinity.
     */
    public static final double CPLEX_POS_INF = Double.MAX_VALUE;
    /**
     * The CPLEX representation of negative infinity.
     */
    public static final double CPLEX_NEG_INF = -Double.MAX_VALUE;
    
    /**
     * The cutoff point at which to treat the value as positive infinity.
     */
    public static final double CPLEX_POS_INF_CUTOFF = 1e19;
    /**
     * The cutoff point at which to treat the value as negative infinity.
     */
    public static final double CPLEX_NEG_INF_CUTOFF = -1e19;

    public static boolean isInfinite(double v) {
        if (v < CPLEX_NEG_INF_CUTOFF || CPLEX_POS_INF_CUTOFF < v) {
            return true;
        }
        return false;
    }
    
    public static double safeGetBound(double bound) {
        if (bound == Double.NEGATIVE_INFINITY) {
            return CPLEX_NEG_INF;
        } else if (bound == Double.POSITIVE_INFINITY) {
            return CPLEX_POS_INF;
        } else {
            return bound;
        }
    }

    public static double[] safeGetBounds(double[] bounds) {
        double[] safeBounds = new double[bounds.length];
        for (int i=0; i<bounds.length; i++) {
            safeBounds[i] = safeGetBound(bounds[i]);
        }
        return safeBounds;
    }
    
    /**
     * Helper method for getting a 3D array of CPLEX variables.
     * 
     * @throws IloException
     */
    public static double[][][] getValues(IloCplex cplex, IloNumVar[][][] vars) throws IloException {
        double[][][] vals = new double[vars.length][][];
        for (int i = 0; i < vars.length; i++) {
            vals[i] = getValues(cplex, vars[i]);
        }
        return vals;
    }

    /**
     * Helper method for getting a 2D array of CPLEX variables.
     * 
     * @throws IloException
     */
    public static double[][] getValues(IloCplex cplex, IloNumVar[][] vars) throws IloException {
        double[][] vals = new double[vars.length][];
        for (int i = 0; i < vars.length; i++) {
            vals[i] = getValues(cplex, vars[i]);
        }
        return vals;
    }

    /**
     * Helper method for getting a 2D array of CPLEX variables.
     * 
     * @throws IloException
     */
    public static double[][] getValues(IloCplex cplex, SymVarMat vars) throws IloException {
        double[][] vals = new double[vars.getNrows()][];
        for (int i = 0; i < vars.getNrows(); i++) {
            IloNumVar[] varsI = vars.getRowAsArray(i);
            vals[i] = getValues(cplex, varsI);
        }
        return vals;
    }

    /**
     * Helper method for getting a 1D array of CPLEX variables.
     * 
     * @throws IloException
     */
    public static double[] getValues(IloCplex cplex, IloNumVar[] vars) throws IloException {
        double[] vals = new double[vars.length];
        for (int i = 0; i < vars.length; i++) {
            if (vars[i] != null) {
                vals[i] = cplex.getValue(vars[i]);
            } else {
                vals[i] = 0.0; // TODO: Double.NaN;
            }
        }
        return vals;
    }

    public static void addRows(IloLPMatrix mat, IloRange[][][] ranges) throws IloException {
        if (ranges == null) {
            return;
        }
        for (int i = 0; i < ranges.length; i++) {
            addRows(mat, ranges[i]);
        }
    }

    public static void addRows(IloLPMatrix mat, IloRange[][] ranges) throws IloException {
        if (ranges == null) {
            return;
		}
		int numNonNull = 0;
		for (int i = 0; i < ranges.length; i++) {
			for (int j = 0; j < ranges[i].length; j++) {
				if (ranges[i][j] != null) {
					numNonNull++;
				}
			}
		}
		int cur = 0;
		IloRange[] nonNulls = new IloRange[numNonNull];
		for (int i = 0; i < ranges.length; i++) {
			for (int j = 0; j < ranges[i].length; j++) {

				if (ranges[i][j] != null) {
					nonNulls[cur++] = ranges[i][j];
				}
			}
		}
		mat.addRows(nonNulls);
    }

    public static void addRows(IloLPMatrix mat, IloRange[] ranges) throws IloException {
        if (ranges == null) {
            return;
        }
        int numNonNull = 0;
        for (int i = 0; i < ranges.length; i++) {
        	if (ranges[i] != null) {
        		numNonNull++;
        	}
        }
        int cur = 0;
        IloRange[] nonNulls = new IloRange[numNonNull];
        for (int i = 0; i < ranges.length; i++) {
        	if (ranges[i] != null) {
        		nonNulls[cur++] = ranges[i];
        	}
        }
        mat.addRows(nonNulls);
    }

    public static void addRow(IloLPMatrix mat, IloRange range) throws IloException {
        if (range == null) {
            return;
        }
        mat.addRow(range);
    }

    // -------- JUnit Assertions -----------

    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow) throws IloException {
        double delta = 1e-3;
        assertContainsRow(rltMat, denseRow, delta);
    }

    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow, double delta) throws IloException {
        int nCols = rltMat.getNcols();
        assertTrue(nCols == denseRow.length);
        int nRows = rltMat.getNrows();
        double[] lbs = new double[nRows];
        double[] ubs = new double[nRows];
        int[][] ind = new int[nRows][];
        double[][] val = new double[nRows][];
        rltMat.getRows(0, nRows, lbs, ubs, ind, val);

        IntDoubleSortedVector expectedRow = new IntDoubleSortedVector(denseRow);

        for (int m = 0; m < nRows; m++) {
            IntDoubleSort.sortIndexAsc(ind[m], val[m]);
            IntDoubleSortedVector row = new IntDoubleSortedVector(ind[m], val[m]);
            // System.out.println(row + "\n" + expectedRow + "\n" +
            // row.equals(expectedRow, 1e-13));
            if (row.eq(expectedRow, delta)) {
                return;
            }
        }
        Assert.fail("Matrix does not contain row: " + Arrays.toString(denseRow));
    }

    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow, double lb, double ub)
            throws IloException {
        double delta = 1e-3;
        assertContainsRow(rltMat, denseRow, lb, ub, delta);
    }

    public static void assertContainsRow(IloLPMatrix rltMat, double[] denseRow, double lb, double ub, double delta)
            throws IloException {
        int nCols = rltMat.getNcols();
        assertTrue(nCols == denseRow.length);
        int nRows = rltMat.getNrows();
        double[] lbs = new double[nRows];
        double[] ubs = new double[nRows];
        int[][] ind = new int[nRows][];
        double[][] val = new double[nRows][];
        rltMat.getRows(0, nRows, lbs, ubs, ind, val);

        IntDoubleSortedVector expectedRow = new IntDoubleSortedVector(denseRow);

        for (int m = 0; m < nRows; m++) {
            IntDoubleSort.sortIndexAsc(ind[m], val[m]);
            IntDoubleSortedVector row = new IntDoubleSortedVector(ind[m], val[m]);
            // System.out.println(row + "\n" + expectedRow + "\n" +
            // row.equals(expectedRow, 1e-13));
            if (row.eq(expectedRow, delta) && Utilities.equals(lb, lbs[m], delta)
                    && Utilities.equals(ub, ubs[m], delta)) {
                return;
            }
        }
        Assert.fail("Matrix does not contain row: " + Arrays.toString(denseRow));
    }

    /**
     * Gets the upper bound of the product of two variables.
     */
    public static double getUpperBound(IloNumVar var1, IloNumVar var2) throws IloException {
        double[] prods = getProductsOfBounds(var1, var2);
        double max = Vectors.max(prods);
        assert (CPLEX_NEG_INF_CUTOFF < max);
        if (isInfinite(max)) {
            return CplexUtils.CPLEX_POS_INF;
        } else {
            return max;
        }
    }

    /**
     * Gets the lower bound of the product of two variables.
     */
    public static double getLowerBound(IloNumVar var1, IloNumVar var2) throws IloException {
        double[] prods = getProductsOfBounds(var1, var2);
        double min = Vectors.min(prods);
        assert (min < CPLEX_POS_INF_CUTOFF);
        if (isInfinite(min)) {
            return CplexUtils.CPLEX_NEG_INF;
        } else {
            return min;
        }
    }

    /**
     * Gets all possible products of the variables' bounds.
     */
    private static double[] getProductsOfBounds(IloNumVar var1, IloNumVar var2) throws IloException {
        double[] prods = new double[4];
        prods[0] = var1.getLB() * var2.getLB();
        prods[1] = var1.getLB() * var2.getUB();
        prods[2] = var1.getUB() * var2.getLB();
        prods[3] = var1.getUB() * var2.getUB();
        return prods;
    }

    /**
     * Gets the dual objective value from CPLEX.
     * 
     * This method is currently broken. We would need to test it on a CPLEX
     * problem where we can do early stopping as we do in RLT. Then we could
     * compare against the objective value given by the dual simplex algorithm,
     * which (it turns out) is exactly what we want anyway.
     * 
     * This might help: 
     * http://www.or-exchange.com/questions/1113/cplex-attributing-dual-values-to-lhsrhs-constraints
     */
    @Deprecated
    public static double getDualObjectiveValue(IloCplex cplex, IloLPMatrix mat) throws IloException {
        if (!cplex.isDualFeasible()) {
            throw new IllegalStateException("No objective value");
        }
        double[] duals = cplex.getDuals(mat);
        double[] redCosts = cplex.getReducedCosts(mat);
        IloNumVar[] numVars = mat.getNumVars();
        BasisStatus[] varBasis = cplex.getBasisStatuses(numVars);
        BasisStatus[] conBasis = cplex.getBasisStatuses(mat.getRanges());

        int numRows = mat.getNrows();
        double[] lb = new double[numRows];
        double[] ub = new double[numRows];
        int[][] Aind = new int[numRows][];
        double[][] Aval = new double[numRows][];
        mat.getRows(0, numRows, lb, ub, Aind, Aval);

        double dualObjVal = 0.0;
        for (int i = 0; i < duals.length; i++) {
            if (Utilities.equals(lb[i], ub[i], 1e-13) && !isInfinite(lb[i])) {
                dualObjVal += duals[i] * lb[i];
            } else {
                if (!isInfinite(lb[i]) && conBasis[i] == BasisStatus.AtLower) {
                    dualObjVal += duals[i] * lb[i];
                } else if (!isInfinite(ub[i]) && conBasis[i] == BasisStatus.AtUpper) {
                    dualObjVal += duals[i] * ub[i];
                } else if (conBasis[i] == BasisStatus.AtUpper || conBasis[i] == BasisStatus.AtLower) {
                    // In certain cases, the BasisStatus for a constraint says
                    // AtLower, but it has a finite upper bound and an infinite
                    // lower bound.
                    if (!isInfinite(lb[i]) && isInfinite(ub[i])) {
                        dualObjVal += duals[i] * lb[i];
                    } else if (isInfinite(lb[i]) && !isInfinite(ub[i])) {
                        dualObjVal += duals[i] * ub[i];
                    }
                }
            }
        }

        for (int i = 0; i < redCosts.length; i++) {
            double varLb = numVars[i].getLB();
            double varUb = numVars[i].getUB();
            if (varBasis[i] == BasisStatus.AtLower && !Utilities.equals(varLb, 0.0, 1e-13) && !isInfinite(varLb)) {
                dualObjVal += redCosts[i] * varLb;
            } else if (varBasis[i] == BasisStatus.AtUpper && !Utilities.equals(varUb, 0.0, 1e-13) && !isInfinite(varUb)) {
                dualObjVal += redCosts[i] * varUb;
            } else if (varBasis[i] == BasisStatus.AtLower || varBasis[i] == BasisStatus.AtUpper) {
                if (!isInfinite(varLb) && isInfinite(varUb)) {
                    dualObjVal += redCosts[i] * varLb;
                } else if (isInfinite(varLb) && !isInfinite(varUb)) {
                    dualObjVal += redCosts[i] * varUb;
                }
            }
        }

        return dualObjVal;
    }

    public static String getMatrixStats(IloLPMatrix mat) throws IloException {
        return String.format("%s contains %d rows, %d columns, %d nonzeros", mat.getName(), mat.getNrows(), mat.getNcols(), mat.getNNZs());
    }

}
