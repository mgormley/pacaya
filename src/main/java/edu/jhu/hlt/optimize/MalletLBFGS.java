package edu.jhu.hlt.optimize;

import java.util.Arrays;

import org.apache.log4j.Logger;

import cc.mallet.optimize.BackTrackLineSearch;
import cc.mallet.optimize.BetterLimitedMemoryBFGS;
import cc.mallet.optimize.InvalidOptimizableException;
import cc.mallet.optimize.Optimizable;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;

/**
 * Wrapper of Mallet's L-BFGS implementation.
 * @author mgormley
 */
public class MalletLBFGS implements Optimizer<DifferentiableFunction> {

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

        private DifferentiableFunction function;
        private IntDoubleVector params;
        private IntDoubleVector gradient;
        private double value; 
        private boolean areGradientAndValueCached;
        
        public MalletFunction(DifferentiableFunction function, IntDoubleVector point) {
            this.function = function;
            params = point;
            gradient = null;
            areGradientAndValueCached = false;
        }
        
        @Override
        public int getNumParameters() {
            return function.getNumDimensions();
        }

        @Override
        public double getParameter(int i) {
            return params.get(i);
        }

        @Override
        public void getParameters(double[] buffer) {
            // Copy the parameters from params to the buffer.
            for (int i=0; i<buffer.length; i++) {
                buffer[i] = params.get(i);
            }
        }

        @Override
        public void setParameter(int i, double value) {
            params.set(i, value);
            // Invalidate the cached gradient.
            areGradientAndValueCached = false;
        }

        @Override
        public void setParameters(double[] buffer) {
            // Copy the parameters from the buffer to params.
            for (int i=0; i<buffer.length; i++) {
                params.set(i, buffer[i]);
            }
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
            for (int i=0; i<buffer.length; i++) {
                buffer[i] = gradient.get(i);
            }
        }

        private void maybeUpdateGradientAndValue() {
            if (!areGradientAndValueCached) {
                // Recompute the value:
                value = function.getValue(params);
                // Recompute the gradient.
                gradient = function.getGradient(params);
                areGradientAndValueCached = true;
            }
        }
        
    }

    private static final Logger log = Logger.getLogger(MalletLBFGS.class);

    private MalletLBFGSPrm prm;
    private boolean converged;
    
    public MalletLBFGS(MalletLBFGSPrm prm) {
        this.prm = prm;
    }
    
    @Override
    public boolean maximize(DifferentiableFunction function, IntDoubleVector point) {
        MalletFunction mf = new MalletFunction(function, point);
        
        BetterLimitedMemoryBFGS lbfgs = new BetterLimitedMemoryBFGS(mf);
        lbfgs.setTolerance(prm.tolerance);
        lbfgs.setGradientTolerance(prm.gradientTolerance);
        
        BackTrackLineSearch btls = new BackTrackLineSearch(mf);
        btls.setAbsTolx(prm.lsAbsTolx);
        btls.setRelTolx(prm.lsRelTolx);
        lbfgs.setLineOptimizer(btls);
        
        try {
            converged = lbfgs.optimize(prm.maxIterations);
        } catch (InvalidOptimizableException e) {
            log.warn("Error during optimization: " + e.getMessage());
            log.warn("Continuing as if there were no error.");
            converged = false;
        }
        
        return converged;
    }

    @Override
    public boolean minimize(DifferentiableFunction function, IntDoubleVector point) {
        return maximize(new DifferentiableFunctionOpts.NegateFunction(function), point);
    }

}
