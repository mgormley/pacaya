package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;

import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Sort;
import edu.jhu.hltcoe.util.Utilities;

public class ModelParamSubproblem {

    // Allow for some floating point error in logadd
    static final double MAX_LOG_SUM = 1e-9;
    static final double MIN_MASS_REMAINING = -1e-10;

    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    public static Pair<double[][], Double> solveModelParamSubproblem(double[][] weights, DmvBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;

        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = weights[c].length;
            logProbs[c] = new double[numParams];

            double massRemaining = 1.0;

            // Initialize each parameter to its lower bound
            for (int m = 0; m < numParams; m++) {
                logProbs[c][m] = bounds.getLb(c, m);
                massRemaining -= Utilities.exp(logProbs[c][m]);
            }

            if (massRemaining < MIN_MASS_REMAINING) {
                // The problem is infeasible
                return null;
            }

            // Find the parameter with the highest cost (most negative) and
            // increase that model
            // parameter up (making their product smaller) to its upper bound or
            // to the amount of mass
            // remaining.
            double[] ws = Utilities.copyOf(weights[c]);
            int[] indices = Sort.getIndexArray(ws);
            Sort.quicksortAsc(ws, indices);
            for (int i = 0; i < indices.length && massRemaining > 0; i++) {
                int m = indices[i];
                double logMax = Utilities.logAdd(Utilities.log(massRemaining), logProbs[c][m]);
                // double logMax = Utilities.log(massRemaining +
                // Utilities.exp(logProbs[c][m]));
                double diff = Math.min(logMax, bounds.getUb(c, m)) - logProbs[c][m];
                massRemaining += Utilities.exp(logProbs[c][m]);
                logProbs[c][m] += diff;
                massRemaining -= Utilities.exp(logProbs[c][m]);
            }

            assert !(massRemaining < MIN_MASS_REMAINING) : "massRemaining=" + massRemaining;

            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }

    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    private Pair<double[][], Double> solveModelParamSubproblem2(double[][] weights, DmvBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;

        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = weights[c].length;
            logProbs[c] = new double[numParams];

            double logSum = Double.NEGATIVE_INFINITY;

            // Initialize each parameter to its upper bound
            for (int m = 0; m < numParams; m++) {
                logProbs[c][m] = bounds.getUb(c, m);
                logSum = Utilities.logAdd(logSum, logProbs[c][m]);
            }

            if (logSum > MAX_LOG_SUM) {
                // The problem is infeasible
                return null;
            }

            // Find the parameter with the highest weight and decrease that
            // model
            // parameter down to its lower bound or to the amount of excess mass
            // remaining.
            double[] ws = Utilities.copyOf(weights[c]);
            int[] indices = Sort.getIndexArray(ws);
            Sort.quicksortAsc(ws, indices);
            for (int i = 0; i < indices.length && logSum < 0; i++) {
                int m = indices[i];
                double diff = Math.min(-logSum, bounds.getUb(c, m) - logProbs[c][m]);
                logSum = Utilities.logSubtract(logSum, logProbs[c][m]);
                logProbs[c][m] += diff;
                logSum = Utilities.logAdd(logSum, logProbs[c][m]);
            }

            assert (logSum <= MAX_LOG_SUM);

            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }

    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    private Pair<double[][], Double> solveModelParamSubproblemOld(double[][] weights, DmvBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;

        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = weights[c].length;
            logProbs[c] = new double[numParams];

            double logSum = Double.NEGATIVE_INFINITY;

            // Initialize each parameter to its lower bound
            for (int m = 0; m < numParams; m++) {
                logProbs[c][m] = bounds.getLb(c, m);
                logSum = Utilities.logAdd(logSum, logProbs[c][m]);
            }

            if (logSum > MAX_LOG_SUM) {
                // The problem is infeasible
                return null;
            }

            // Find the parameter with the lowest weight and increase that model
            // parameter up to its upper bound or to the amount of mass
            // remaining.
            double[] ws = Utilities.copyOf(weights[c]);
            int[] indices = Sort.getIndexArray(ws);
            Sort.quicksortAsc(ws, indices);
            for (int i = 0; i < indices.length && logSum < 0; i++) {
                int m = indices[i];
                double diff = Math.min(-logSum, bounds.getUb(c, m) - logProbs[c][m]);
                logSum = Utilities.logSubtract(logSum, logProbs[c][m]);
                logProbs[c][m] += diff;
                logSum = Utilities.logAdd(logSum, logProbs[c][m]);
            }

            assert (logSum <= MAX_LOG_SUM);

            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }

}
