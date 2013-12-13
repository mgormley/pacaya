package edu.jhu.globalopt.cpt;

import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.CpxException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.globalopt.cpt.CptBoundsDelta.Type;
import edu.jhu.globalopt.cpt.Projections.ProjectionsPrm.ProjectionType;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.util.math.FastMath;

public class Projections {

    public static class ProjectionsPrm {
        public static enum ProjectionType { BOUNDED_MIN_EUCLIDEAN, UNBOUNDED_MIN_EUCLIDEAN, NORMALIZE };
        public ProjectionType type = ProjectionType.UNBOUNDED_MIN_EUCLIDEAN;
        public File tempDir = null;
        public double lambdaSmoothing = 0.0;
    }
    
    private static final Logger log = Logger.getLogger(Projections.class);

    private IloCplex cplex;
    private ProjectionsPrm prm;

    public Projections(ProjectionsPrm prm) {
        this.prm = prm;
        try {
            cplex = new IloCplex();
            // Turn off stdout but not stderr
            cplex.setOut(null);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param logBounds Bounds for log probabilities
     * @param c Index of distribution which has bounds
     * @param params Vector to project onto (params.length - 1)-simplex in probability space 
     * @return The projected parameters or null if infeasible
     */
    public double[] getDefaultProjection(CptBounds logBounds, int c, double[] params) throws IloException {
        if (prm.lambdaSmoothing != 0) {
            params = Arrays.copyOf(params, params.length);
            DoubleArrays.add(params, prm.lambdaSmoothing);
        }
        if (prm.type == ProjectionType.BOUNDED_MIN_EUCLIDEAN) {
            return getBoundedProjection(logBounds, c, params);
        } else if (prm.type == ProjectionType.UNBOUNDED_MIN_EUCLIDEAN) {
            return getUnboundedProjection(params);
        } else if (prm.type == ProjectionType.NORMALIZE) {
            return getNormalizedProjection(params, prm.lambdaSmoothing);
        } else {
            throw new IllegalStateException("Unhandled projection type: " + prm.type);
        }
        
    }

    /**
     * Projects the vector onto the simplex by renormalizing it, optionally with smoothing.
     * 
     * @param params Vector to project onto (params.length - 1)-simplex in probability space
     * @param lambda Add-lambda smoothing parameter. 
     * @return The parameters projected onto the simplex
     */
    public static double[] getNormalizedProjection(double[] params, double lambda) {
        params = Arrays.copyOf(params, params.length);
        Multinomials.normalizeProps(params);
        return params;
    }

    /**
     * Implementation of Algorithm 1 (projsplx) from "Projection Onto A Simplex"
     * http://arxiv.org/abs/1101.6081
     * 
     * @param params Vector to project onto (params.length - 1)-simplex in probability space 
     * @return The parameters projected onto the simplex
     */
    public static double[] getUnboundedProjection(double[] params) {
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
     * @param params Vector to project onto (params.length - 1)-simplex in probability space 
     * @return The projected parameters or null if infeasible
     */
    public double[] getBoundedProjection(CptBounds logBounds, int c, double[] params) throws IloException {
        double[] lbs = new double[params.length];
        double[] ubs = new double[params.length];
        for (int m = 0; m < params.length; m++) {
            lbs[m] = FastMath.exp(logBounds.getLb(Type.PARAM, c, m));
            ubs[m] = FastMath.exp(logBounds.getUb(Type.PARAM, c, m));
        }
        
        return getBoundedProjection(params, lbs, ubs);
    }

    /**
     * @param params Vector to project onto (params.length - 1)-simplex in probability space 
     * @param lbs Lower bounds in probability space
     * @param ubs Upper bounds in probability space
     * @return The projected parameters or null if infeasible
     */
    public double[] getBoundedProjection(double[] params, double[] lbs, double[] ubs) throws IloException,
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


        if (prm.tempDir != null) {
            cplex.exportModel(new File(prm.tempDir, "proj.lp").getAbsolutePath());
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

}
