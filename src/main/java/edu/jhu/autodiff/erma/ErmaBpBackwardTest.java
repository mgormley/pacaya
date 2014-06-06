package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.Messages;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactorTest;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactorTest.FgAndLinks;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.Prng;


public class ErmaBpBackwardTest {

    private static boolean logDomain = false;
    
    @Test
    public void testErmaGradientOneVarAssertions() {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        fg.addFactor(emit0);

        emit0.setValue(0, FastMath.log(1.1));
        emit0.setValue(1, FastMath.log(1.9));      
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(t0, 1);
                
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.forward();        
        bp.getOutputAdj().varBeliefs[0].setValue(0, 1.0);
        bp.getOutputAdj().varBeliefs[0].setValue(1, 0.0);
        bp.backward();        
        {
            assertEquals(2, bp.getMessagesAdj().length);
            Messages adj0 = bp.getMessagesAdj()[0];
            assertEquals(0, adj0.message.getValue(0), 1e-3);
            assertEquals(0, adj0.message.getValue(1), 1e-3);
            assertEquals(1.181, adj0.newMessage.getValue(0), 1e-3);
            assertEquals(-0.175, adj0.newMessage.getValue(1), 1e-3);
            
            Messages adj1 = bp.getMessagesAdj()[1];
            assertEquals(0, adj1.message.getValue(0), 1e-3);
            assertEquals(0, adj1.message.getValue(1), 1e-3);
            assertEquals(0, adj1.newMessage.getValue(0), 1e-3);
            assertEquals(0, adj1.newMessage.getValue(1), 1e-3);
        }
        {
            assertEquals(1, bp.getPotentialsAdj().length);
            VarTensor adj = bp.getPotentialsAdj()[0];
            assertEquals(1.181, adj.getValue(0), 1e-3);
            assertEquals(-0.175, adj.getValue(1), 1e-3);
        }
    }
    
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
        fn.getGradient(ModuleTestUtils.getAbsZeroOneGaussian(fn.getNumDimensions()));
        
        testGradientByFiniteDifferences(fn);
        testGradientBySpsaApprox(fn);
    }
    
    // Tests ErmaBp and ExpectedRecall gradient by finite differences on a small chain factor graph.
    @Test
    public void testErmaGradientLinearChain() {
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars();
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
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars();
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

    @Test
    public void testErmaGradient1WordGlobalFactor() {
        double[] root = new double[]{ 1.0 };
        double[][] child = new double[][]{ { 0.0 } };
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl(root, child);
        FactorGraph fg = fgl.fg;
        LinkVar[] rootVars = fgl.rootVars;
        LinkVar[][] childVars = fgl.childVars;
                
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(rootVars[0], 1);
        ErmaErFn fn = new ErmaErFn(fg, goldConfig);
        
        testGradientByFiniteDifferences(fn);
    }
    
    @Test
    public void testErmaGradient2WordExplicitTreeFactor() {
        ErmaErFn fnExpl = getErmaFnFor2WordSent(true);        
        ErmaErFn fnDp = getErmaFnFor2WordSent(false);
        assertEquals(fnExpl.getNumDimensions(), fnDp.getNumDimensions());
        IntDoubleVector x = ModuleTestUtils.getAbsZeroOneGaussian(fnExpl.getNumDimensions());
        assertEquals(fnExpl.getValue(x), fnDp.getValue(x), 1e-13);
        IntDoubleVector gradExpl = fnExpl.getGradient(x);
        IntDoubleVector gradDp = fnDp.getGradient(x);
        for (int c=0; c<gradExpl.getNumImplicitEntries(); c++) {
            assertEquals(gradExpl.get(c), gradDp.get(c), 1e-13);
        }
        
        fdWithOrWithoutExplicit(true);
        fdWithOrWithoutExplicit(false);
    }

    private void fdWithOrWithoutExplicit(boolean useExplicit) {
        ErmaErFn fn = getErmaFnFor2WordSent(useExplicit);        
        testGradientByFiniteDifferences(fn);
    }

    private ErmaErFn getErmaFnFor2WordSent(boolean useExplicit) {
        FgAndLinks fgl = ProjDepTreeFactorTest.get2WordSentFgAndLinks(logDomain, useExplicit, false, false);
        FactorGraph fg = fgl.fg;
        LinkVar[] rootVars = fgl.rootVars;
        LinkVar[][] childVars = fgl.childVars;
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(rootVars[0], 0);
        goldConfig.put(rootVars[1], 1);
        goldConfig.put(childVars[0][1], 0);
        goldConfig.put(childVars[1][0], 1);
        ErmaErFn fn = new ErmaErFn(fg, goldConfig);
        return fn;
    }
    
    @Test
    public void testErmaGradient2WordGlobalFactor() {
        double[] root = new double[]{ 1.0, 1.0 };
        double[][] child = new double[][]{ { 0.0, 1.0 }, { 1.0, 0.0 } };
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl(root, child);
        FactorGraph fg = fgl.fg;
        LinkVar[] rootVars = fgl.rootVars;
        LinkVar[][] childVars = fgl.childVars;
                
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(rootVars[0], 0);
        goldConfig.put(rootVars[1], 1);
        goldConfig.put(childVars[0][1], 0);
        goldConfig.put(childVars[1][0], 1);
        ErmaErFn fn = new ErmaErFn(fg, goldConfig);
        
        testGradientByFiniteDifferences(fn);
    }
    
    @Test
    public void testErmaGradient3WordGlobalFactor() {
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl(logDomain);
        FactorGraph fg = fgl.fg;
        LinkVar[] rootVars = fgl.rootVars;
        LinkVar[][] childVars = fgl.childVars;
                
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(rootVars[0], 0);
        goldConfig.put(rootVars[1], 1);
        goldConfig.put(rootVars[2], 0);
        goldConfig.put(childVars[0][1], 0);
        goldConfig.put(childVars[0][2], 0);
        goldConfig.put(childVars[1][0], 1);
        goldConfig.put(childVars[1][2], 1);
        goldConfig.put(childVars[2][0], 0);
        goldConfig.put(childVars[2][1], 0);
       
        ErmaErFn fn = new ErmaErFn(fg, goldConfig);
        
        testGradientByFiniteDifferences(fn);
    }

    private static void testGradientByFiniteDifferences(ErmaErFn fn) {
        Prng.seed(12345);
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        System.out.println("theta0 = " + theta0);
        Prng.seed(System.currentTimeMillis());
        
        ModuleTestUtils.assertFdAndAdEqual(fn, theta0, 1e-5, 1e-7);
    }
    
    private static IntDoubleVector testGradientBySpsaApprox(ErmaErFn fn) {
        Prng.seed(12345);
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
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
                if (f instanceof GlobalFactor || f.getVars().size() == 4) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
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
            er.getOutputAdj().fill(1);
            er.backward();
            bp.backward();
            
            VarTensor[] potentialsAdj = bp.getPotentialsAdj();
            IntDoubleVector grad = new IntDoubleDenseVector(numParams);
            int i=0;
            for (int a=0; a<potentialsAdj.length; a++) {
                Factor f = fg.getFactor(a);
                if (f instanceof GlobalFactor || f.getVars().size() == 4) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
                // Skip GlobalFactors.
                if (potentialsAdj[a] != null) {
                    for (int c=0; c<potentialsAdj[a].size(); c++) {
                        grad.set(i++, potentialsAdj[a].getValue(c));
                    }
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
            return er.forward().getValue(0);
        }

        private void updateFactorGraphFromModel(IntDoubleVector theta) {
            // Update factors from the model in params array.
            int i=0;
            for (Factor f : fg.getFactors()) {
                if (f instanceof GlobalFactor || f.getVars().size() == 4) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
                ExplicitFactor factor = (ExplicitFactor) f;
                for (int c=0; c<factor.size(); c++) {
                    factor.setValue(c, theta.get(i++));
                }
            }
        }
        
    }
    
}
