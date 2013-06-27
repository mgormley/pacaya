package edu.jhu.optimize;

import cc.mallet.optimize.BackTrackLineSearch;
import cc.mallet.optimize.BetterLimitedMemoryBFGS;
import cc.mallet.optimize.Optimizable;

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
        private boolean isGradientCached; 
        
        public MalletFunction(Function function) {
            this.function = function;
            params = new double[function.getNumDimensions()];
            gradient = new double[function.getNumDimensions()];
            isGradientCached = false;
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
            isGradientCached = false;
        }

        @Override
        public void setParameters(double[] buffer) {
            // Copy the parameters from the buffer to params.
            System.arraycopy(buffer, 0, params, 0, params.length);
            // Invalidate the cached gradient.
            isGradientCached = false;
        }

        @Override
        public double getValue() {
            return function.getValue(params);
        }

        @Override
        public void getValueGradient(double[] buffer) {
            if (!isGradientCached) {
                // Recompute the gradient.
                gradient = function.getGradient(params);
            }            
            // Copy the gradient to the buffer.
            System.arraycopy(gradient, 0, buffer, 0, gradient.length);            
        }
        
    }
    
    private MalletLBFGSPrm prm;
    private boolean converged;
    
    public MalletLBFGS(MalletLBFGSPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public double[] maximize(Function function, double[] initial) {
        MalletFunction mf = new MalletFunction(function);
        mf.setParameters(initial);
        
        BetterLimitedMemoryBFGS lbfgs = new BetterLimitedMemoryBFGS(mf);
        lbfgs.setTolerance(prm.tolerance);
        lbfgs.setGradientTolerance(prm.gradientTolerance);
        
        BackTrackLineSearch btls = new BackTrackLineSearch(mf);
        btls.setAbsTolx(prm.lsAbsTolx);
        btls.setRelTolx(prm.lsRelTolx);
        lbfgs.setLineOptimizer(btls);
        
        converged = lbfgs.optimize(prm.maxIterations);
        
        return mf.params;
    }

    @Override
    public boolean wasMaxima() {
        return converged;
    }

}
