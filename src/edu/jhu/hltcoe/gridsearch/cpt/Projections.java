package edu.jhu.hltcoe.gridsearch.cpt;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.CpxException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import depparsing.model.NonterminalMap;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.cky.DepInstance;
import edu.jhu.hltcoe.parse.cky.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Utilities;

public class Projections {

    private static final Logger log = Logger.getLogger(Projections.class);

    private IloCplex cplex;
    private File tempDir;

    public Projections() {
        this(null);
    }
    
    public Projections(File tempDir) {
        this.tempDir = tempDir;
        try {
            cplex = new IloCplex();
            // Turn off stdout but not stderr
            cplex.setOut(null);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }    
    }

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
    
    /**
     * @param logBounds Bounds for log probabilities
     * @param c Index of distribution which has bounds
     * @param params Vector to project onto (param.length - 1)-simplex in probability space
     * @return The projected parameters or null if infeasible
     */
    public double[] getProjectedParams(CptBounds logBounds, int c, double[] params) throws IloException {
        double[] lbs = new double[params.length];
        double[] ubs = new double[params.length];
        for (int m = 0; m < params.length; m++) {
            lbs[m] = Utilities.exp(logBounds.getLb(Type.PARAM, c, m));
            ubs[m] = Utilities.exp(logBounds.getUb(Type.PARAM, c, m));
        }
        
        return getProjectedParams(params, lbs, ubs);
    }

    /**
     * @param params Vector to project onto (param.length - 1)-simplex in probability space
     * @param lbs Lower bounds in probability space
     * @param ubs Upper bounds in probability space
     * @return The projected parameters or null if infeasible
     */
    public double[] getProjectedParams(double[] params, double[] lbs, double[] ubs) throws IloException,
            UnknownObjectException {
        cplex.clearModel();
        
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


        if (tempDir != null) {
            cplex.exportModel(new File(tempDir, "proj.lp").getAbsolutePath());
        }
        try {
            if (!cplex.solve()) {
                // throw new RuntimeException("projection infeasible");
                return null;
            }
        } catch (CpxException e) {
            log.error("params: " + Arrays.toString(params));
            log.error("lbs: " + Arrays.toString(lbs));
            log.error("ubs: " + Arrays.toString(ubs));
            throw e;
        }

        double[] values = cplex.getValues(newParamVars);
        for (int m=0; m<values.length; m++) {
            if (values[m] < -1e-8) {
                log.warn("Oddly low value after projection: values[m] = " + values[m]);
            }
            if (values[m] < 0.0) {
                values[m] = 0.0;
            }
        }
        return values;
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

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

}
