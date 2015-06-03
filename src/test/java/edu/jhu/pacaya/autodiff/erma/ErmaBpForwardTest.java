package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.inf.BruteForceInferencerTest;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphTest;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.pacaya.util.semiring.ShiftedRealAlgebra;
import edu.jhu.pacaya.util.semiring.SplitAlgebra;


public class ErmaBpForwardTest {
	
    @Test
    public void testOnOneVarProb() {
        testOneVarHelper(RealAlgebra.REAL_ALGEBRA);
    }
    
    @Test
    public void testOnOneVarLogProb() {
        Algebra s = LogSemiring.LOG_SEMIRING;
        testOneVarHelper(s);
    }

    private void testOneVarHelper(Algebra s) {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);

        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 

        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);

        fg.addFactor(emit0);
        
        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();

        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 10;
        prm.s = s;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();

        assertEqualMarginals(fg, bf, bp);
    }
    
    @Test
    public void testTwoVars() {
        runTwoVars(RealAlgebra.REAL_ALGEBRA, null);
        runTwoVars(LogSemiring.LOG_SEMIRING, null);
    }

    @Test
    public void testDumpingOfBeliefsForDebugging() {
        runTwoVars(RealAlgebra.REAL_ALGEBRA, Paths.get("./tmp/bpDump"));
        // No assertions, just make sure we don't fail.
    }

    private void runTwoVars(Algebra s, Path dumpDir) {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);

        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);

        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        tran0.fill(1);
        tran0.setValue(0, 2.2);
        tran0.setValue(1, 2.3);
        tran0.setValue(2, 2.4);
        tran0.setValue(3, 2.5);
        
        fg.addFactor(emit0);
        fg.addFactor(tran0);
        
        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();

        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 10;
        prm.s = s;
        prm.dumpDir = dumpDir;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();

        assertEqualMarginals(fg, bf, bp);
    }
    
    @Test
    public void testThreeConnectedComponents() {
        
        Algebra s = LogSemiring.LOG_SEMIRING;
        
        FactorGraph fg = getThreeConnectedComponentsFactorGraph();
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();

        ErmaBp bp = runDefaultBpForAcyclic(s, fg);

        assertEqualMarginals(fg, bf, bp);
    }

    public static FactorGraph getThreeConnectedComponentsFactorGraph() {
        FactorGraph fg = new FactorGraph();
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Lists.getList("N", "V"));
        
        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0));; 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1));; 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2));; 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 3);
        emit1.setValue(1, 7);
        emit2.setValue(0, 1);
        emit2.setValue(1, 1);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);

        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        
        return fg;
    }
    
    @Test
    public void testOnSimpleProb() throws IOException {
        // Test in the probability domain.
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        testOnSimpleHelper(s);
    }
    
    @Test
    public void testOnSimpleLogProb() throws IOException {
        // Test in the log-probability domain.
        Algebra s = LogSemiring.LOG_SEMIRING;        
        testOnSimpleHelper(s);
    }

    private void testOnSimpleHelper(Algebra s) throws IOException {
        FactorGraph fg = BruteForceInferencerTest.readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();

        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 10;
        prm.s = s;
        prm.normalizeMessages = true;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();

        //BruteForceInferencerTest.testInfOnSimpleGraph(fg, bp, s);

        // TODO: unfortunately, loopy BP does very poorly on this simple example
        // and does not converge to the correct marginals. Hence we use a (very
        // high) tolerance of 2 to catch the partition function's value.
        assertEqualMarginals(fg, bf, bp, 2);
    }
    
    @Test
    public void testMultipleSemiringsOnSimple() throws IOException {
        FactorGraph fg = BruteForceInferencerTest.readSimpleFg();

        ErmaBp bpReal = runHelper(fg, RealAlgebra.REAL_ALGEBRA);
        ErmaBp bpSplit = runHelper(fg, SplitAlgebra.SPLIT_ALGEBRA);
        ErmaBp bpShift = runHelper(fg, ShiftedRealAlgebra.SHIFTED_REAL_ALGEBRA);
        ErmaBp bpLog = runHelper(fg, LogSemiring.LOG_SEMIRING);
        ErmaBp bpLogSign = runHelper(fg, LogSignAlgebra.LOG_SIGN_ALGEBRA);
        
        assertEqualMarginals(fg, bpReal, bpSplit, 1e-4);
        assertEqualMarginals(fg, bpReal, bpShift, 1e-13);
        assertEqualMarginals(fg, bpReal, bpLog, 1e-13);
        assertEqualMarginals(fg, bpReal, bpLogSign, 1e-13);
        assertEqualMarginals(fg, bpLog, bpLogSign, 1e-13);
    }

    private ErmaBp runHelper(FactorGraph fg, Algebra s) throws IOException {        
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 4;
        prm.s = s;
        prm.normalizeMessages = true;
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();
        return bp;
    }

    @Test
    public void testOnChainProb() {
        // Test in the probability domain.
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        testOnChainHelper(s);
    }

    @Test
    public void testOnChainLogProb() {
        // Test in the log-probability domain.
        Algebra s = LogSemiring.LOG_SEMIRING;        
        testOnChainHelper(s);
    }

    private void testOnChainHelper(Algebra s) {
        FactorGraph fg = BruteForceInferencerTest.getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();

        ErmaBp bp = runDefaultBpForAcyclic(s, fg);

        BruteForceInferencerTest.testInfOnLinearChainGraph(fg, bp);
                    
        assertEqualMarginals(fg, bf, bp);
    }

    protected ErmaBp runDefaultBpForAcyclic(Algebra s, FactorGraph fg) {
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 1;
        prm.s = s;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        // Don't normalize the messages, so that the partition function is the
        // same as in the brute force approach.
        prm.normalizeMessages = false;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();
        return bp;
    }
    
    @Test
    public void testConvergence() {
        // Test with a threshold of 0 (i.e. exact equality implies convergence)
        testConvergenceHelper(LogSemiring.LOG_SEMIRING, 0, 6);
        testConvergenceHelper(RealAlgebra.REAL_ALGEBRA, 0, 6);
        // Test with a threshold of 1e-3 (i.e. fewer iterations, 5, to convergence)
        testConvergenceHelper(LogSemiring.LOG_SEMIRING, 1e-3, 5);
        testConvergenceHelper(RealAlgebra.REAL_ALGEBRA, 1e-3, 5);
    }

    private void testConvergenceHelper(Algebra s, double convergenceThreshold, int expectedConvergenceIterations) {
        FactorGraph fg = BruteForceInferencerTest.getLinearChainGraph();

        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();
        
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 1;
        prm.s = s;
        prm.normalizeMessages = true;
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.convergenceThreshold = convergenceThreshold;        
        
        ErmaBp bp = null;
        
        for (int i=0; i<20; i++) {
            prm.maxIterations = i;
            bp = new ErmaBp(fg, prm);
            bp.run();
            System.out.println("maxiters: " + i);
            System.out.println("isConverged: " + bp.isConverged());
            if (bp.isConverged()) {
                assertEqualMarginals(fg, bf, bp, convergenceThreshold + 1e-13);
                assertTrue(prm.maxIterations >= expectedConvergenceIterations);
            } else {
                assertTrue(prm.maxIterations < expectedConvergenceIterations);
                try {
                    assertEqualMarginals(fg, bf, bp);
                    fail("Marginals should not be equal");
                } catch (AssertionError e) {
                    // pass
                }
            }
        }
        assertTrue(bp.isConverged());
    }
    
    @Test
    public void testCanHandleProbHardFactors() {
        //TODO: ErmaBp doesn't currently do factor belief caching. 
        // testCanHandleHardFactorsHelper(true, RealAlgebra.REAL_ALGEBRA);
        testCanHandleHardFactorsHelper(false, RealAlgebra.REAL_ALGEBRA);
    }
    
    @Test
    public void testCanHandleLogHardFactors() {
        //TODO: ErmaBp doesn't currently do factor belief caching. 
        // testCanHandleHardFactorsHelper(true, LogSemiring.LOG_SEMIRING);
        testCanHandleHardFactorsHelper(false, LogSemiring.LOG_SEMIRING);
    }    
    
    public void testCanHandleHardFactorsHelper(boolean cacheFactorBeliefs, Algebra s) {     
        Var x0 = new Var(VarType.PREDICTED, 2, "x0", null);
        Var x1 = new Var(VarType.PREDICTED, 2, "x1", null);
        
        ExplicitFactor xor = new ExplicitFactor(new VarSet(x0, x1));
        for(int cfg=0; cfg < xor.getVars().calcNumConfigs(); cfg++) {
            VarConfig vCfg = xor.getVars().getVarConfig(cfg);
            int v0 = vCfg.getState(x0);
            int v1 = vCfg.getState(x1);
            if(v0 != v1)
                xor.setValue(cfg, 0d);
            else
                xor.setValue(cfg, 1d);
        }
        xor.convertRealToLog();
        
        FactorGraph fg = new FactorGraph();
        fg.addVar(x0);
        fg.addVar(x1);
        fg.addFactor(xor);
        
        // should have uniform mass
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();
        ErmaBpPrm prm = new ErmaBpPrm();
        prm.maxIterations = 10;
        prm.s = s;
        //TODO: prm.cacheFactorBeliefs = cacheFactorBeliefs;
        ErmaBp bp = new ErmaBp(fg, prm);
        bp.run();
        System.out.println(bp.isConverged());
        assertEqualMarginals(fg, bf, bp);
        
        VarTensor x0_marg = bp.getMarginals(x0);
        assertEquals(0.5d, x0_marg.getValue(0), 1e-6);
        assertEquals(0.5d, x0_marg.getValue(1), 1e-6);
        VarTensor x1_marg = bp.getMarginals(x1);
        assertEquals(0.5d, x1_marg.getValue(0), 1e-6);
        assertEquals(0.5d, x1_marg.getValue(1), 1e-6);
                
        // check again once we've added some unary factors on x0 and x1
        ExplicitFactor f0 = new ExplicitFactor(new VarSet(x0));
        f0.setValue(0, 3d);
        f0.setValue(1, 2d);
        f0.convertRealToLog();
        fg.addFactor(f0);
        
        ExplicitFactor f1 = new ExplicitFactor(new VarSet(x0));
        f1.setValue(0, 5d);
        f1.setValue(1, 1d);
        f1.convertRealToLog();
        fg.addFactor(f1);
        
        bf = new BruteForceInferencer(fg, s);
        bf.run();
        bp = new ErmaBp(fg, prm);
        bp.run();
        System.out.println(bp.isConverged());
        assertEqualMarginals(fg, bf, bp);
    }    
    
    @Test
    public void testGlobalExplicitFactor() throws IOException {
        FactorGraph fg = getThreeConnectedComponentsFactorGraph();
        
        VarSet allVars = new VarSet(fg.getVars().toArray(new Var[0]));
        ExplicitFactor gf = new GlobalExplicitFactor(allVars);
        gf.setValue(0, 2);
        gf.setValue(1, 3);
        gf.setValue(2, 5);
        gf.setValue(3, 7);
        gf.setValue(4, 11);
        gf.setValue(5, 15);
        gf.setValue(6, 19);
        gf.setValue(7, 23);
        gf.convertRealToLog();
        
        fg.addFactor(gf);

        Algebra s = LogSemiring.LOG_SEMIRING;
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();
        ErmaBp bp = runDefaultBpForAcyclic(s, fg);
        System.out.println(bp.isConverged());
        assertEqualMarginals(fg, bf, bp);
    }

    public static void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            ErmaBp bp) {
        assertEqualMarginals(fg, bf, bp, 1e-13);
    }

    public static void assertEqualMarginals(FactorGraph fg, FgInferencer bf,
            FgInferencer bp, double tolerance) {
        for (Var var : fg.getVars()) {
            {
                VarTensor bfm = bf.getMarginals(var);
                VarTensor bpm = bp.getMarginals(var);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
            {
                VarTensor bfm = bf.getLogMarginals(var);
                VarTensor bpm = bp.getLogMarginals(var);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
        }
        for (Factor f : fg.getFactors()) {
            {
                VarTensor bfm = bf.getMarginals(f);
                VarTensor bpm = bp.getMarginals(f);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
            {
                VarTensor bfm = bf.getLogMarginals(f);
                VarTensor bpm = bp.getLogMarginals(f);
                if (!bfm.equals(bpm, tolerance)) {
                    assertEquals(bfm, bpm);
                }
            }
        }
        assertEquals(bf.getPartition(), bp.getPartition(), tolerance);
        assertEquals(bf.getLogPartition(), bp.getLogPartition(), tolerance);
    }
    
}
