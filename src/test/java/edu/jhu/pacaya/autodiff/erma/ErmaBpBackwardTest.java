package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.StochasticGradientApproximation;
import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactorTest;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactorTest.FgAndLinks;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.prim.util.random.Prng;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;


public class ErmaBpBackwardTest {

    private static Algebra s = RealAlgebra.getInstance();
    
    @Before
    public void setUp() {
        Prng.seed(1l);
    }
    
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
        prm.maxIterations = 1;
        prm.s = s;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.forward();        
        bp.getOutputAdj().varBeliefs[0].setValue(0, 1.0);
        bp.getOutputAdj().varBeliefs[0].setValue(1, 0.0);
        bp.backward();        
        {
            assertEquals(2, bp.getMessagesAdj().length);
            VarTensor adj0 = bp.getMessagesAdj()[1];
            VarTensor newAdj0 = bp.getNewMessagesAdj()[1];
            assertEquals(0, adj0.getValue(0), 1e-3);
            assertEquals(0, adj0.getValue(1), 1e-3);
            assertEquals(0.211, newAdj0.getValue(0), 1e-3);
            assertEquals(-0.122, newAdj0.getValue(1), 1e-3);
            
            VarTensor adj1 = bp.getMessagesAdj()[0];
            VarTensor newAdj1 = bp.getNewMessagesAdj()[0];
            assertEquals(0, adj1.getValue(0), 1e-3);
            assertEquals(0, adj1.getValue(1), 1e-3);
            assertEquals(0, newAdj1.getValue(0), 1e-3);
            assertEquals(0, newAdj1.getValue(1), 1e-3);
        }
        {
            assertEquals(1, bp.getPotentialsAdj().length);
            VarTensor adj = bp.getPotentialsAdj()[0];
            assertEquals(0.211, adj.getValue(0), 1e-3);
            assertEquals(-0.122, adj.getValue(1), 1e-3);
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
        
        testGradientByFiniteDifferences(fg, goldConfig);
        testGradientBySpsaApprox(new ErmaErFn(fg, goldConfig));
    }

    // Tests ErmaBp and ExpectedRecall gradient by finite differences on a small chain factor graph.
    @Test
    public void testErmaGradientLinearChain() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();
        FactorGraph fg = fgv.fg;
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
                
        testGradientByFiniteDifferences(fg, goldConfig);
    }
    
    // Tests that the adjoints for ErmaBp are equal for both PARALLEL and SEQUENTIAL schedules.
    @Test
    public void testErmaGradientLinearChainParallelSequential() {
        //FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars();
        //final FactorGraph fg = fgv.fg;
        
        final FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1));
        fg.addFactor(emit0);
        fg.addFactor(tran0);
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(t0, 1);
        
        // Inputs        
        FgModelIdentity modIn = new FgModelIdentity(new FgModel(0));
        // The sampled values will be in the real semiring.
        FactorsModule effm = new FactorsModule(modIn, fg, RealAlgebra.getInstance());
        effm.forward();
        
        // SEQUENTIAL TREE_LIKE
        OneToOneFactory<Factors,Beliefs> fact1 = new OneToOneFactory<Factors,Beliefs>() {
            public Module<Beliefs> getModule(Module<Factors> m1) {
                ErmaBpPrm prm = ErmaErFn.getDefaultErmaBpPrm();
                prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
                prm.schedule = BpScheduleType.TREE_LIKE;
                prm.normalizeMessages = false;
                return new ErmaBp(fg, prm, m1);
            }
        };
        
        // PARALLEL TREE_LIKE
        OneToOneFactory<Factors,Beliefs> fact2 = new OneToOneFactory<Factors,Beliefs>() {
            public Module<Beliefs> getModule(Module<Factors> m1) {
                ErmaBpPrm prm = ErmaErFn.getDefaultErmaBpPrm();
                prm.updateOrder = BpUpdateOrder.PARALLEL;
                prm.schedule = BpScheduleType.TREE_LIKE;
                prm.normalizeMessages = false;
                prm.maxIterations = 100;
                return new ErmaBp(fg, prm, m1);
            }
        };
        
        AbstractModuleTest.checkOneToOneEqualAdjointsAbs(fact1, fact2, effm);
    }
    
    // TODO: This test is really slow: ~20 seconds.
    @Test
    public void testErmaGradientLinearChainWithLoops() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();
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
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.maxIterations = 2;
        prm.s = s;
        prm.normalizeMessages = true;
        
        testGradientByFiniteDifferences(fg, goldConfig, prm);
    }
    
    @Test
    public void testErmaGradientWithGlobalExplicitFactor() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();
        FactorGraph fg = fgv.fg;
        
        ExplicitFactor loop0 = new GlobalExplicitFactor(new VarSet(fgv.t0, fgv.t1, fgv.t2)); 
        loop0.setValue(0, 2);
        loop0.setValue(1, 3);
        loop0.setValue(2, 5);
        loop0.setValue(3, 7);
        loop0.setValue(4, 11);
        loop0.setValue(5, 15);
        loop0.setValue(6, 19);
        loop0.setValue(7, 23);
        
        fg.addFactor(loop0);
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
        
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.maxIterations = 10;
        prm.s = s;
        prm.normalizeMessages = true;
        
        testGradientByFiniteDifferences(fg, goldConfig, prm);
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
        testGradientByFiniteDifferences(fg, goldConfig);
    }

    // Tests that the adjoints for ErmaBp are equal when the global factor does and does NOT use its
    // dynamic programming approach..
    @Test
    public void testErmaGradient2WordGlobalFactorVsExplicit() {
        boolean useExplicit = false;
        FgAndLinks fgl = ProjDepTreeFactorTest.get2WordSentFgAndLinks(useExplicit, false, false);
        final FactorGraph fg = fgl.fg;
        
        // Inputs        
        FgModelIdentity modIn = new FgModelIdentity(new FgModel(0));
        // The sampled values will be in the real semiring.
        FactorsModule effm = new FactorsModule(modIn, fg, RealAlgebra.getInstance());
        effm.forward();
        
        // SEQUENTIAL TREE_LIKE
        OneToOneFactory<Factors,Beliefs> fact1 = new OneToOneFactory<Factors,Beliefs>() {
            public Module<Beliefs> getModule(Module<Factors> m1) {
                ErmaBpPrm prm = ErmaErFn.getDefaultErmaBpPrm();
                prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
                prm.schedule = BpScheduleType.TREE_LIKE;
                prm.maxIterations = 100;
                return new ErmaBp(fg, prm, m1);
            }
        };
        
        // SEQUENTIAL NO_GLOBAL_FACTORS
        OneToOneFactory<Factors,Beliefs> fact2 = new OneToOneFactory<Factors,Beliefs>() {
            public Module<Beliefs> getModule(Module<Factors> m1) {
                ErmaBpPrm prm = ErmaErFn.getDefaultErmaBpPrm();
                prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
                prm.schedule = BpScheduleType.NO_GLOBAL_FACTORS;
                prm.maxIterations = 100;
                return new ErmaBp(fg, prm, m1);
            }
        };
        
        AbstractModuleTest.checkOneToOneEqualAdjointsAbs(fact1, fact2, effm);
    }
    
    @Ignore("This test fails at the first assertion because the number of dimensions is different for the expilicit and dynamic programming functions.")
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
        FgAndLinks fgl = ProjDepTreeFactorTest.get2WordSentFgAndLinks(useExplicit, false, false);
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
        testGradientByFiniteDifferences(fg, goldConfig);
    }
    
    @Test
    public void testErmaGradient3WordGlobalFactor() {
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl();
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
        
        testGradientByFiniteDifferences(fg, goldConfig);
    }

    
    private static void testGradientByFiniteDifferences(FactorGraph fg, VarConfig goldConfig) {
        testGradientByFiniteDifferences(fg, goldConfig, ErmaErFn.getDefaultErmaBpPrm());
    }
    
    private static void testGradientByFiniteDifferences(final FactorGraph fg, final VarConfig goldConfig, final ErmaBpPrm prm) {
        // Test BP and Expected Recall together.
        testGradientByFiniteDifferences(new ErmaErFn(fg, goldConfig, prm));
                
        // Inputs        
        FgModelIdentity modIn = new FgModelIdentity(new FgModel(0));
        // The sampled values will be in the real semiring.
        FactorsModule effm = new FactorsModule(modIn, fg, RealAlgebra.getInstance());
        effm.forward();
        
        // Test BP and Expected Recall together.
        //        OneToOneFactory<Factors,Tensor> fact1 = new OneToOneFactory<Factors,Tensor>() {
        //            public Module<Tensor> getModule(Module<Factors> m1) {
        //                ErmaBp bp = new ErmaBp(fg, prm, m1);
        //                ExpectedRecall er = new ExpectedRecall(bp, goldConfig);
        //                return new TopoOrder<Tensor>(Lists.getList(m1), er);                
        //            }
        //        };
        //        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact1, effm);
        
        // Test BP by itself.
        OneToOneFactory<Factors,Beliefs> fact2 = new OneToOneFactory<Factors,Beliefs>() {
            public Module<Beliefs> getModule(Module<Factors> m1) {
                return new ErmaBp(fg, prm, m1);
            }
        };
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact2, effm);
    }

    private static void testGradientByFiniteDifferences(ErmaErFn fn) {
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        System.out.println("theta0 = " + theta0);
        
        ModuleTestUtils.assertGradientCorrectByFd(fn, theta0, 1e-5, 1e-7);
    }
    
    private static IntDoubleVector testGradientBySpsaApprox(ErmaErFn fn) {
        int numParams = fn.getNumDimensions();
        IntDoubleVector theta0 = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        System.out.println("theta0 = " + theta0);
        
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
            numParams = getNumParams(fg);
        }

        private static ErmaBpPrm getDefaultErmaBpPrm() {
            ErmaBpPrm prm = new ErmaBpPrm();
            prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
            prm.schedule = BpScheduleType.TREE_LIKE;
            prm.maxIterations = 1;
            prm.s = s;
            prm.normalizeMessages = true;
            return prm;
        }
        
        @Override
        public double getValue(IntDoubleVector theta) {
            return runForward(theta);
        }

        private double runForward(IntDoubleVector theta) {
            updateFactorGraphFromModel(theta, fg);
            bp = new ErmaBp(fg, prm);
            bp.forward();            
            er = new ExpectedRecall(bp, goldConfig);            
            return er.forward().getValue(0);
        }
        
        @Override
        public int getNumDimensions() {
            return numParams;
        }

        /** Get the gradient by running AD. */
        @Override
        public IntDoubleVector getGradient(IntDoubleVector theta) {
            runForward(theta);
            er.getOutputAdj().fill(s.one());
            er.backward();
            bp.backward();
            
            return getGradientFromPotentialsAdj(bp, fg, numParams);
        }

        @Override
        public ValueGradient getValueGradient(IntDoubleVector point) {
            return new ValueGradient(getValue(point), getGradient(point));
        }

        private static int getNumParams(FactorGraph fg) {
            int numParams = 0;
            for (Factor f : fg.getFactors()) {
                if (isGlobalFactor(f)) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
                numParams += f.getVars().calcNumConfigs();
            }
            return numParams;
        }
        
        /** Assumes that theta is in the real semiring. */
        private static void updateFactorGraphFromModel(IntDoubleVector theta, FactorGraph fg) {
            // Update factors from the model in params array.
            int i=0;
            for (Factor f : fg.getFactors()) {
                if (isGlobalFactor(f)) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
                ExplicitFactor factor = (ExplicitFactor) f;
                for (int c=0; c<factor.size(); c++) {
                    double logValue = FastMath.log(theta.get(i++));
                    // IMPORTANT NOTE: we set the factor to be the log of the score.
                    factor.setValue(c, logValue);
                }
            }
        }

        /** Gets the gradient in the real semiring. */
        private static IntDoubleVector getGradientFromPotentialsAdj(ErmaBp bp, FactorGraph fg, int numParams) {
            VarTensor[] potentialsAdj = bp.getPotentialsAdj();
            IntDoubleVector grad = new IntDoubleDenseVector(numParams);
            int i=0;
            for (int a=0; a<potentialsAdj.length; a++) {
                Factor f = fg.getFactor(a);
                if (isGlobalFactor(f)) {
                    System.out.println("Skipping factor: " + f);
                    continue;
                }
                if (potentialsAdj[a] != null) {
                    for (int c=0; c<potentialsAdj[a].size(); c++) {
                        grad.set(i++, s.toReal(potentialsAdj[a].getValue(c)));
                    }
                }
            }
            assert i == numParams;
            return grad;
        }

        private static boolean isGlobalFactor(Factor f) {
            // TODO: || f.getVars().size() == 4; This was used for the explicit tree factor. Is
            // there some way to incorporate something that marks a factor as global without making
            // BP treat it as one.
            return f instanceof GlobalFactor; 
        }
        
    }
    
}
