package edu.jhu.optimize;

import cc.mallet.optimize.BackTrackLineSearch;
import cc.mallet.optimize.BetterLimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;
import edu.jhu.prim.util.Utilities;

public class MalletLBFGS implements Maximizer {

    public static class MalletLBFGSPrm {
        /** Max iterations. */
        public int maxIterations = 1000; 
        /** Function value tolerance. */
        public double tolerance = .0001;
        /** Gradient tolerance.*/ 
        public double gradientTolerance = .001;
        /** Number of corrections. */
        public int numberOfCorrections = 4;
        
        /** termination conditions: either
         *   a) abs(delta x/x) < REL_TOLX for all coordinates
         *   b) abs(delta x) < ABS_TOLX for all coordinates
         *   c) sufficient function increase (uses ALF)
         */
        public double lsRelTolx = 1e-7;
        /** tolerance on absolute value difference */
        public double lsAbsTolx = 1e-4; 
    }
    
    /**
     * Wrapper of edu.jhu.Function to implement Optimizable.ByGradientValue.
     * 
     * @author mgormley
     *
     */
    private static class MalletFunction implements Optimizable.ByGradientValue {

        private Function function;
        private double[] params;
        private double[] gradient;
        private double value; 
        private boolean areGradientAndValueCached;
        
        public MalletFunction(Function function) {
            this.function = function;
            params = new double[function.getNumDimensions()];
            gradient = new double[function.getNumDimensions()];
            areGradientAndValueCached = false;
        }
        
        @Override
        public int getNumParameters() {
            return function.getNumDimensions();
        }

        @Override
        public double getParameter(int i) {
            return params[i];
        }

        @Override
        public void getParameters(double[] buffer) {
            // Copy the parameters from params to the buffer.
            System.arraycopy(params, 0, buffer, 0, params.length);
        }

        @Override
        public void setParameter(int i, double value) {
            params[i] = value;
            // Invalidate the cached gradient.
            areGradientAndValueCached = false;
        }

        @Override
        public void setParameters(double[] buffer) {
            // Copy the parameters from the buffer to params.
            System.arraycopy(buffer, 0, params, 0, params.length);
            // Invalidate the cached gradient.
            areGradientAndValueCached = false;
        }

        @Override
        public double getValue() {
            maybeUpdateGradientAndValue();
            return value;
        }

        @Override
        public void getValueGradient(double[] buffer) {
            maybeUpdateGradientAndValue();            
            // Copy the gradient to the buffer.
            System.arraycopy(gradient, 0, buffer, 0, gradient.length);            
        }

        private void maybeUpdateGradientAndValue() {
            if (!areGradientAndValueCached) {
                function.setPoint(params);
                // Recompute the value:
                value = function.getValue();
                // Recompute the gradient.
                function.getGradient(gradient);
                areGradientAndValueCached = true;
            }
        }
        
    }
    
    private MalletLBFGSPrm prm;
    private boolean converged;
    
    public MalletLBFGS(MalletLBFGSPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public boolean maximize(Function function, double[] point) {
        MalletFunction mf = new MalletFunction(function);
        mf.setParameters(point);
        
        BetterLimitedMemoryBFGS lbfgs = new BetterLimitedMemoryBFGS(mf);
        lbfgs.setTolerance(prm.tolerance);
        lbfgs.setGradientTolerance(prm.gradientTolerance);
        
        BackTrackLineSearch btls = new BackTrackLineSearch(mf);
        btls.setAbsTolx(prm.lsAbsTolx);
        btls.setRelTolx(prm.lsRelTolx);
        lbfgs.setLineOptimizer(btls);
        
        converged = lbfgs.optimize(prm.maxIterations);
        
        Utilities.copy(mf.params, point);
        
        return converged;
    }

}
