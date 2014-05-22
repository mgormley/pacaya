package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.apple.mrj.macos.carbon.Timer;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Prng;
import edu.jhu.util.dist.Gaussian;


public class ErmaBpBackwardTest {

    private static boolean logDomain = false;
    
    @Test
    public void testErmaGradientOneVar() {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        fg.addFactor(emit0);
                
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(t0, 1);
                
        ErmaErFn fn = new ErmaErFn(fg, goldConfig); 
        Prng.seed(12345);
        fn.getGradient(getTheta(fn.getNumDimensions()));
        
        IntDoubleVector gradAd = testGradientByFiniteDifferences(fn);
        //assertEquals(new double[]{-0.034560412986688674, 0.8756722814502975}, );
    }
    
    // Tests ErmaBp and ExpectedRecall gradient by finite differences on a small chain factor graph.
    @Test
    public void testErmaGradientLinearChain() {
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(logDomain);
        FactorGraph fg = fgv.fg;
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
                
        ErmaErFn fn = new ErmaErFn(fg, goldConfig);
        
        testGradientByFiniteDifferences(fn);
    }
    
    @Test
    public void testErmaGradientLinearChainWithLoops() {
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(logDomain);
        FactorGraph fg = fgv.fg;
        
        ExplicitFactor loop0 = new ExplicitFactor(new VarSet(fgv.t0, fgv.t2)); 
        ExplicitFactor loop1 = new ExplicitFactor(new VarSet(fgv.w0, fgv.w2)); 
        ExplicitFactor loop2 = new ExplicitFactor(new VarSet(fgv.t0, fgv.t1, fgv.t2)); 
        ExplicitFactor loop3 = new ExplicitFactor(new VarSet(fgv.w0, fgv.w1, fgv.w2)); 

        fg.addFactor(loop0);
        fg.addFactor(loop1);
        fg.addFactor(loop2);
        fg.addFactor(loop3);
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
                
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        prm.normalizeMessages = true;
        
        ErmaErFn fn = new ErmaErFn(fg, goldConfig, prm);
        
        testGradientByFiniteDifferences(fn);
    }

    private static IntDoubleVector testGradientByFiniteDifferences(ErmaErFn fn) {
        Prng.seed(12345);
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = getTheta(numParams);
        System.out.println("theta0 = " + theta0);
        Prng.seed(System.currentTimeMillis());
        
        IntDoubleVector gradFd0 = StochasticGradientApproximation.estimateGradientSpsa(fn, theta0, 1000);      
        IntDoubleVector gradFd1 = StochasticGradientApproximation.estimateGradientSpsa(fn, theta0, 1000);      
        IntDoubleVector gradAd = fn.getGradient(theta0);
        System.out.println("gradFd = " + gradFd0);
        System.out.println("gradFd = " + gradFd1);
        System.out.println("gradAd = " + gradAd);
        
        double infNormFd = infNorm(gradFd0, gradFd1);
        double infNorm = infNorm(gradFd0, gradAd);
        System.out.println("infNorm(gradFd0, gradFd1) = " + infNormFd);
        System.out.println("infNorm(gradFd0, gradAd) = " + infNorm);
        assertTrue(infNorm < 1e-1);
        
        return gradAd;
    }

    private static double infNorm(IntDoubleVector gradFd0, IntDoubleVector gradAd) {
        IntDoubleDenseVector diff = new IntDoubleDenseVector(gradFd0);
        diff.subtract(gradAd);        
        double infNorm = diff.infinityNorm();
        return infNorm;
    }

    private static IntDoubleDenseVector getTheta(int numParams) {
        // Define the "model" as the explicit factor entries.
        IntDoubleDenseVector theta = new IntDoubleDenseVector(numParams);
        // Randomly initialize the model.
        for (int i=0; i< numParams; i++) {
            theta.set(i, Math.abs(Gaussian.nextDouble(0.0, 1.0)));
        }
        return theta;
    }
    
    private static class ErmaErFn implements DifferentiableFunction {

        private FactorGraph fg;
        private VarConfig goldConfig;
        private int numParams;
        private ErmaBp bp;
        private ExpectedRecall er;
        private ErmaBpPrm prm;
        
        public ErmaErFn(FactorGraph fg, VarConfig goldConfig) {
            this(fg, goldConfig, getDefaultErmaBpPrm());
        }

        public ErmaErFn(FactorGraph fg, VarConfig goldConfig, ErmaBpPrm prm) {
            this.fg = fg;
            this.goldConfig = goldConfig;
            this.prm = prm;
            cacheNumParams();
        }

        private static ErmaBpPrm getDefaultErmaBpPrm() {
            ErmaBpPrm prm = new ErmaBpPrm();
            prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
            prm.schedule = BpScheduleType.TREE_LIKE;
            prm.maxIterations = 1;
            prm.logDomain = logDomain;
            prm.normalizeMessages = true;
            return prm;
        }

        private void cacheNumParams() {
            numParams = 0;
            for (Factor f : fg.getFactors()) {
                numParams += f.getVars().calcNumConfigs();
            }
        }
        
        @Override
        public double getValue(IntDoubleVector theta) {
            return runForward(theta);
        }
        
        @Override
        public int getNumDimensions() {
            return numParams;
        }

        /** Get the gradient by running AD. */
        @Override
        public IntDoubleVector getGradient(IntDoubleVector theta) {
            runForward(theta);
            er.backward(1);
            bp.backward(er.getVarBeliefsAdjs(), er.getFacBeliefsAdjs());
            
            VarTensor[] potentialsAdj = bp.getPotentialsAdj();
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

        private double runForward(IntDoubleVector theta) {
            updateFactorGraphFromModel(theta);
            bp = new ErmaBp(fg, prm);
            bp.forward();            
            er = new ExpectedRecall(bp, goldConfig);
            er.forward();
            return er.getExpectedRecall();
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
        
    }

}
