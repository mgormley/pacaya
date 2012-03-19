package edu.jhu.hltcoe.gridsearch.dmv;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.util.Arrays;

import depparsing.model.NonterminalMap;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;

public class Projections {

    /**
     * Implementation of Algorithm 1 (projsplx) from "Projection Onto A Simplex"
     * http://arxiv.org/abs/1101.6081
     * 
     * @param params
     *            The input parameters
     * @return The parameters projected onto the simplex
     */
    public static double[] getProjectedParams(double[] params) {
        double[] sortedParams = Arrays.copyOf(params, params.length);
        Arrays.sort(sortedParams);
        int n = params.length;

        double that = Double.POSITIVE_INFINITY;
        for (int i = n - 1; i >= 0; i--) {
            double ti = 0.0;
            for (int j = i + 1; j <= n; j++) {
                ti += sortedParams[j - 1];
            }
            ti -= 1.0;
            ti /= n - i;
            // System.out.printf("t_{%d} = %f\n", i, ti);
            if (i == 0 || ti >= sortedParams[i - 1]) {
                that = ti;
                break;
            }
        }
        // System.out.printf("t_hat = %f\n", that);

        // Just re-use the sortedParams array instead of reallocating memory
        double[] newParams = sortedParams;
        for (int i = 0; i < newParams.length; i++) {
            newParams[i] = params[i] - that;
            if (newParams[i] < 0.0) {
                newParams[i] = 0.0;
            }
        }

        return newParams;
    }

    public static double[] getProjectedParams(DmvBounds bounds, int c, double[] params) throws IloException {
        double[] lbs = new double[params.length];
        double[] ubs = new double[params.length];
        for (int m = 0; m < params.length; m++) {
            lbs[m] = bounds.getLb(c, m);
            ubs[m] = bounds.getUb(c, m);
        }

        return getProjectedParams(params, lbs, ubs);
    }

    public static double[] getProjectedParams(double[] params, double[] lbs, double[] ubs) throws IloException,
            UnknownObjectException {
        IloCplex cplex = new IloCplex();

        IloNumVar[] newParamVars = new IloNumVar[params.length];
        for (int m = 0; m < newParamVars.length; m++) {
            newParamVars[m] = cplex.numVar(lbs[m], ubs[m], String.format("p_{%d}", m));
        }

        cplex.addEq(cplex.sum(newParamVars), 1.0, "sum-to-one");

        IloNumExpr[] squaredDiffs = new IloNumExpr[params.length];
        for (int m = 0; m < squaredDiffs.length; m++) {
            squaredDiffs[m] = cplex
                    .prod(cplex.diff(params[m], newParamVars[m]), cplex.diff(params[m], newParamVars[m]));
        }

        cplex.addMinimize(cplex.sum(squaredDiffs), "obj");

        if (!cplex.solve()) {
            throw new RuntimeException("projection infeasible");
        }

        return cplex.getValues(newParamVars);
    }

    public static DepTree getProjectiveParse(Sentence sentence, double[] fracRoot, double[][] fracChild) {
        DmvCkyParser parser = new DmvCkyParser();
        int[] tags = new int[sentence.size()];
        DepInstance depInstance = new DepInstance(tags);
        DepSentenceDist sd = new DepSentenceDist(depInstance, new NonterminalMap(2, 1), fracRoot, fracChild);
        Pair<DepTree, Double> pair = parser.parse(sentence, sd);
        DepTree tree = pair.get1();
        return tree;
    }

}
