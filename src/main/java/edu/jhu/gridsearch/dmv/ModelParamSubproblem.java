package edu.jhu.gridsearch.dmv;

import org.apache.log4j.Logger;

import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;

import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.StrictlyConvexMultivariateRealFunction;
import com.joptimizer.optimizers.CvxOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.joptimizer.optimizers.OptimizationResponse;
import com.joptimizer.optimizers.PrimalDualMethod;

import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.util.sort.IntDoubleSort;
import edu.jhu.util.Pair;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

public class ModelParamSubproblem {
    
    private static final Logger log = Logger.getLogger(ModelParamSubproblem.class);

    
    // Allow for some floating point error in logadd
    static final double MAX_LOG_SUM = 1e-9;
    static final double MIN_MASS_REMAINING = -1e-10;

    private Algebra ALG = Algebra.DEFAULT;
    private DoubleFactory1D F1 = DoubleFactory1D.dense;
    private DoubleFactory2D F2 = DoubleFactory2D.dense;


    
    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    public Pair<double[][], Double> solveModelParamSubproblemJOptimizeProb(double[][] weights, CptBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;
        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            logProbs[c] = solveModelParamSubproblemJOptimizeProb(weights[c], bounds, c);
            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }
    
    public double[] solveModelParamSubproblemJOptimizeProb(final double[] weights, CptBounds bounds, int c) {
        final int numParams = weights.length;
        
        // Log-probability space objective
        StrictlyConvexMultivariateRealFunction objectiveFunction = new StrictlyConvexMultivariateRealFunction() {

            public double value(DoubleMatrix1D X) {
                double value = 0.0;
                for (int m=0; m<numParams; m++) {
                    double xm = X.getQuick(m);
                    if (weights[m] > 0.0) {
                        if (xm < 0) {
                            value += weights[m] * Utilities.log(0.0);
                        } else {
                            value += weights[m] * Utilities.log(xm);
                        }
                    }
                }
                if (value == Double.NEGATIVE_INFINITY) {
                    value = -Double.MAX_VALUE;
                }
                assert(!Double.isNaN(value) && !Double.isInfinite(value));
                return value;
            }

            public DoubleMatrix1D gradient(DoubleMatrix1D X) {
                double[] gradient = new double[numParams];
                for (int m=0; m<numParams; m++) {
                    gradient[m] = weights[m] / X.getQuick(m);
                }
                return F1.make(gradient);
            }

            public DoubleMatrix2D hessian(DoubleMatrix1D X) {
                DoubleMatrix2D hessian = F2.make(numParams, numParams);
                for (int m=0; m<numParams; m++) {
                    double xm = X.getQuick(m);
                    double value = -weights[m] / (xm * xm);
                    assert(!Double.isNaN(value));
                    hessian.setQuick(m, m, value);
                }
                return hessian;
            }

            public int getDim() {
                return numParams;
            }
        };

        ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[2*numParams];

        // Lower bounds
        for (int m=0; m<numParams; m++) {
            double[] factors = new double[numParams];
            factors[m] = -1;
            inequalities[m] = new LinearMultivariateRealFunction(F1.make(factors), Utilities.exp(bounds.getLb(Type.PARAM, c, m)) - 1e-13);
        }
        
        // Upper bounds
        for (int m=0; m<numParams; m++) {
            double[] factors = new double[numParams];
            factors[m] = 1;
            inequalities[numParams+m] = new LinearMultivariateRealFunction(F1.make(factors), - (Utilities.exp((bounds.getUb(Type.PARAM, c, m)) + 1e-13)));
        }

        OptimizationRequest or = new OptimizationRequest();
        or.f0 = objectiveFunction;
        // TODO: better initialization!!!
        double[] initProbs = new double[numParams];
        for (int m=0; m<numParams; m++) {
            initProbs[m] = Utilities.exp(bounds.getLb(Type.PARAM, c, m));
        }
        double remaining = 1.0 - Vectors.sum(initProbs);
        for (int m=0; m<numParams; m++) {
            double diff = Math.min(remaining, Utilities.exp(bounds.getUb(Type.PARAM, c, m)) - initProbs[m]);
            initProbs[m] += diff;
            remaining -= diff;
            if (remaining <= 0) {
                break;
            }
        }
        // TODO: remove
        //Multinomials.normalizeProps(initProbs);
        //assert Vectors.sum(initProbs) == 1.0 : Arrays.toString(initProbs) + " " + Vectors.sum(initProbs);
        
        or.initialPoint = F1.make(initProbs);
        or.fi = inequalities;    

        // Sum to one constraints
        or.A = F2.make(1, numParams).assign(1.0);
        or.b = F1.make(new double[] { 1 });
                
        // Run optimization
        PrimalDualMethod opt = new PrimalDualMethod();
        opt.setOptimizationRequest(or);
        OptimizationResponse response;
        try {
            response = opt.optimize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(response.returnCode==OptimizationResponse.FAILED){
            // TODO: maybe return infeasible?
            throw new IllegalStateException("Unable to solve model subproblem");
        }
        DoubleMatrix1D sol = response.solution;
        double[] solution = sol.toArray();
        for (int m=0; m<numParams; m++) {
            if (solution[m] < 0) {
                solution[m] = CptBounds.DEFAULT_LOWER_BOUND;
            } else {
                solution[m] = Math.max(Utilities.log(solution[m]), CptBounds.DEFAULT_LOWER_BOUND);
            }
        }
        //Multinomials.normalizeLogProps(solution);
        return solution;
    }
    
    
    
    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    public Pair<double[][], Double> solveModelParamSubproblemJOptimizeLogProb(double[][] weights, CptBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;
        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            logProbs[c] = solveModelParamSubproblemJOptimizeLogProb(weights[c], bounds, c);
            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }
    
    public double[] solveModelParamSubproblemJOptimizeLogProb(final double[] weights, CptBounds bounds, int c) {
        final int numParams = weights.length;
        final DoubleMatrix1D weights1D = F1.make(weights);
        final DoubleMatrix2D zeros2D = F2.make(numParams, numParams);
        
        // Log-probability space objective
        StrictlyConvexMultivariateRealFunction objectiveFunction = new StrictlyConvexMultivariateRealFunction() {

            public double value(DoubleMatrix1D X) {
                return X.zDotProduct(weights1D);
            }

            public DoubleMatrix1D gradient(DoubleMatrix1D X) {
                return weights1D;
            }

            public DoubleMatrix2D hessian(DoubleMatrix1D X) {
                return zeros2D;
            }

            public int getDim() {
                return numParams;
            }
        };

        // Sum to one constraints
        ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[1+2*numParams];
        inequalities[0] = new ConvexMultivariateRealFunction() {
            
            @Override
            public double value(DoubleMatrix1D X) {
                return X.aggregate(Functions.plus, Functions.exp) - 1.0;
            }
            
            @Override
            public DoubleMatrix1D gradient(DoubleMatrix1D X) {
                return X.copy().assign(Functions.exp);
            }
            
            @Override
            public DoubleMatrix2D hessian(DoubleMatrix1D X) {
                DoubleMatrix2D hessian = F2.make(numParams, numParams);
                for (int m=0; m<numParams; m++) {
                    hessian.setQuick(m, m, Math.exp(X.getQuick(m)));
                }
                return hessian;
            }
            
            @Override
            public int getDim() {
                return numParams;
            }
        };
        
        // Lower bounds
        for (int m=0; m<numParams; m++) {
            double[] factors = new double[numParams];
            factors[m] = -1;
            inequalities[1+m] = new LinearMultivariateRealFunction(F1.make(factors), bounds.getLb(Type.PARAM, c, m) - 1e-13);
        }
        
        // Upper bounds
        for (int m=0; m<numParams; m++) {
            double[] factors = new double[numParams];
            factors[m] = 1;
            inequalities[1+numParams+m] = new LinearMultivariateRealFunction(F1.make(factors), -(bounds.getUb(Type.PARAM, c, m) + 1e-13));
        }
        
        // Run optimization
        OptimizationRequest or = new OptimizationRequest();
        or.f0 = objectiveFunction;
        // TODO: better initialization!!!
        double[] initLogProbs = new double[numParams];
        for (int m=0; m<numParams; m++) {
            initLogProbs[m] = bounds.getLb(Type.PARAM, c, m);
        }
        or.initialPoint = F1.make(initLogProbs);
        or.fi = inequalities;

        // optimization
        CvxOptimizer opt = new CvxOptimizer();
        opt.setOptimizationRequest(or);
        OptimizationResponse response;
        try {
            response = opt.optimize();
        } catch (Exception e) {
            if (e.getMessage().contains("initial")) {
                log.error(String.format("sum(initLogProbs) = %e", Vectors.sum(initLogProbs)));
            }
            throw new RuntimeException(e);
        }
        if(response.returnCode==OptimizationResponse.FAILED){
            // TODO: maybe return infeasible?
            throw new IllegalStateException("Unable to solve model subproblem");
        }
        DoubleMatrix1D sol = response.solution;
        return sol.toArray();
    }
    
    /**
     * TODO: remove this is just wrong
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    @Deprecated
    public static Pair<double[][], Double> solveModelParamSubproblem(double[][] weights, CptBounds bounds) {
        int numConds = weights.length;

        double cost = 0.0;

        double[][] logProbs = new double[numConds][];
        for (int c = 0; c < numConds; c++) {
            int numParams = weights[c].length;
            logProbs[c] = new double[numParams];

            double massRemaining = 1.0;

            // Initialize each parameter to its lower bound
            for (int m = 0; m < numParams; m++) {
                logProbs[c][m] = bounds.getLb(Type.PARAM, c, m);
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
            int[] indices = IntDoubleSort.getIntIndexArray(ws);
            IntDoubleSort.sortValuesAsc(ws, indices);
            for (int i = 0; i < indices.length && massRemaining > 0; i++) {
                int m = indices[i];
                double logMax = Utilities.logAdd(Utilities.log(massRemaining), logProbs[c][m]);
                // double logMax = Utilities.log(massRemaining +
                // Utilities.exp(logProbs[c][m]));
                double diff = Math.min(logMax, bounds.getUb(Type.PARAM, c, m)) - logProbs[c][m];
                massRemaining += Utilities.exp(logProbs[c][m]);
                logProbs[c][m] += diff;
                massRemaining -= Utilities.exp(logProbs[c][m]);
            }

            assert !(massRemaining < MIN_MASS_REMAINING) : "massRemaining=" + massRemaining;

            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return new Pair<double[][], Double>(logProbs, cost);
    }
    
    public static double getReducedCost(double[][] weights, double[][] logProbs) {
        double cost = 0.0;
        for (int c = 0; c < logProbs[c].length; c++) {
            cost += Vectors.dotProduct(logProbs[c], weights[c]);
        }
        return cost;
    }
    
}
