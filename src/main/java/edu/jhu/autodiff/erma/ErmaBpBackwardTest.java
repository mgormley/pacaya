package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Prng;
import edu.jhu.util.dist.Gaussian;


public class ErmaBpBackwardTest {
    
    // Tests ErmaBp and ExpectedRecall gradient by finite differences on a small chain factor graph.
    @Test
    public void testGradientMatchesFiniteDifferences() {
        ErmaErFn fn = new ErmaErFn();
        
        Prng.seed(12345);
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = getTheta(numParams);
        Prng.seed(System.currentTimeMillis());
        
        IntDoubleVector gradFd = StochasticGradientApproximation.estimateGradient(fn, theta0);      
        IntDoubleVector gradAd = fn.getGradient(theta0);
        System.out.println(gradFd);
        System.out.println(gradAd);
        
        IntDoubleDenseVector diff = new IntDoubleDenseVector(gradFd);
        diff.subtract(gradAd);
        
        assertTrue(diff.infinityNorm() < 1e-1);
    }

    private IntDoubleDenseVector getTheta(int numParams) {
        // Define the "model" as the explicit factor entries.
        IntDoubleDenseVector theta = new IntDoubleDenseVector(numParams);
        // Randomly initialize the model.
        for (int i=0; i< numParams; i++) {
            theta.set(i, Math.abs(Gaussian.nextDouble(0.0, 1.0)));
        }
        return theta;
    }
    
    private static class ErmaErFn implements DifferentiableFunction {

        boolean logDomain = false;
        private FactorGraph fg;
        private VarConfig goldConfig;
        private int numParams;

        public ErmaErFn() {
            FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(logDomain);
            fg = fgv.fg;
            
            goldConfig = new VarConfig();
            goldConfig.put(fgv.w0, 0);
            goldConfig.put(fgv.w1, 1);
            goldConfig.put(fgv.w2, 0);
            goldConfig.put(fgv.t1, 1);
            goldConfig.put(fgv.t2, 1);
            
            numParams = 0;
            for (Factor f : fg.getFactors()) {
                numParams += f.getVars().calcNumConfigs();
            }
        }
        
        @Override
        public double getValue(IntDoubleVector theta) {
            updateFactorGraphFromModel(theta);
            return getExpectedRecall(logDomain, fg, goldConfig);
        }

        private void updateFactorGraphFromModel(IntDoubleVector theta) {
            // Update factors from the model in params array.
            int i=0;
            for (Factor f : fg.getFactors()) {
                ExplicitFactor factor = (ExplicitFactor) f;
                for (int c=0; c<factor.size(); c++) {
                    factor.setValue(c, theta.get(i++));
                }
            }
        }

        private double getExpectedRecall(boolean logDomain, FactorGraph fg, VarConfig goldConfig) {
            ErmaBpPrm prm = new ErmaBpPrm();
            prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
            prm.schedule = BpScheduleType.TREE_LIKE;
            prm.logDomain = logDomain;
            prm.normalizeMessages = true;
            ErmaBp bp = new ErmaBp(fg, prm);
            bp.forward();
            
            ExpectedRecall er = new ExpectedRecall(bp, goldConfig);
            er.forward();
            
            return er.getExpectedRecall();
        }
        
        @Override
        public int getNumDimensions() {
            return numParams;
        }

        /** Get the gradient by running AD. */
        @Override
        public IntDoubleVector getGradient(IntDoubleVector point) {
            ErmaBpPrm prm = new ErmaBpPrm();
            prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
            prm.schedule = BpScheduleType.TREE_LIKE;
            prm.logDomain = logDomain;
            prm.normalizeMessages = true;
            ErmaBp bp = new ErmaBp(fg, prm);
            bp.forward();
            
            ExpectedRecall er = new ExpectedRecall(bp, goldConfig);
            er.forward();
            
            er.backward(1);
            bp.backward(er.getVarBeliefsAdjs(), er.getFacBeliefsAdjs());
            
            DenseFactor[] potentialsAdj = bp.getPotentialsAdj();
            IntDoubleVector grad = new IntDoubleDenseVector(numParams);
            int i=0;
            for (int a=0; a<potentialsAdj.length; a++) {
                for (int c=0; c<potentialsAdj[a].size(); c++) {
                    grad.set(i++, potentialsAdj[a].getValue(c));
                }
            }
            assert i == numParams;
            return grad;
        }

        @Override
        public ValueGradient getValueGradient(IntDoubleVector point) {
            return new ValueGradient(getValue(point), getGradient(point));
        }
        
    }

}
