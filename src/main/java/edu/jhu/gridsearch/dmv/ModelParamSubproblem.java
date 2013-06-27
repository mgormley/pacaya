package edu.jhu.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.cpt.CptBounds;
import edu.jhu.util.Pair;

public class ModelParamSubproblem {
    
    private static final Logger log = Logger.getLogger(ModelParamSubproblem.class);

    
    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    public Pair<double[][], Double> solveModelParamSubproblemJOptimizeProb(double[][] weights, CptBounds bounds) {
    throw new RuntimeException("function body removed for hrelease"); }
    
    public double[] solveModelParamSubproblemJOptimizeProb(final double[] weights, CptBounds bounds, int c) {
    throw new RuntimeException("function body removed for hrelease"); }
    
    
    
    /**
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    public Pair<double[][], Double> solveModelParamSubproblemJOptimizeLogProb(double[][] weights, CptBounds bounds) {
    throw new RuntimeException("function body removed for hrelease"); }
    
    public double[] solveModelParamSubproblemJOptimizeLogProb(final double[] weights, CptBounds bounds, int c) {
    throw new RuntimeException("function body removed for hrelease"); }
    
    /**
     * TODO: remove this is just wrong
     * Solve the minimization subproblem for the model parameters subject to the
     * sum-to-one constraints and the bounds on the variables.
     */
    @Deprecated
    public static Pair<double[][], Double> solveModelParamSubproblem(double[][] weights, CptBounds bounds) {
    throw new RuntimeException("function body removed for hrelease"); }
    
    public static double getReducedCost(double[][] weights, double[][] logProbs) {
    throw new RuntimeException("function body removed for hrelease"); }
    
}
