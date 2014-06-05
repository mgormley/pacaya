package edu.jhu.gm.inf;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.collections.Lists;


public class BeliefPropagationTest {
	    
    @Test
    public void testOnOneVarProb() {
        boolean logDomain = false;
        testOneVarHelper(logDomain);
    }
    
    @Test
    public void testOnOneVarLogProb() {
        boolean logDomain = true;
        testOneVarHelper(logDomain);
    }

    private void testOneVarHelper(boolean logDomain) {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);

        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 

        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);

        fg.addFactor(emit0);
        
        for (Factor f : fg.getFactors()) {
            ((VarTensor)f).convertRealToLog();
        }
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();

        assertEqualMarginals(fg, bf, bp);
    }
    
    @Test
    public void testTwoVarsProb() {
        boolean logDomain = false;

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
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();

        assertEqualMarginals(fg, bf, bp);
    }
    
    @Test
    public void testThreeConnectedComponents() {
        
        boolean logDomain = true;
        
        FactorGraph fg = getThreeConnectedComponentsFactorGraph();
        
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        // Don't normalize the messages, so that the partition function is the
        // same as in the brute force approach.
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();

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
        boolean logDomain = false;
        testOnSimpleHelper(logDomain);
    }
    
    @Test
    public void testOnSimpleLogProb() throws IOException {
        // Test in the log-probability domain.
        boolean logDomain = true;        
        testOnSimpleHelper(logDomain);
    }

    @Test
    public void testOnChainProb() {
        // Test in the probability domain.
        boolean logDomain = false;
        testOnChainHelper(logDomain);
    }

    @Test
    public void testOnChainLogProb() {
        // Test in the log-probability domain.
        boolean logDomain = true;        
        testOnChainHelper(logDomain);
    }

    private void testOnSimpleHelper(boolean logDomain) throws IOException {
        FactorGraph fg = BruteForceInferencerTest.readSimpleFg(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        prm.normalizeMessages = true;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();

        //BruteForceInferencerTest.testInfOnSimpleGraph(fg, bp, logDomain);

        // TODO: unfortunately, loopy BP does very poorly on this simple example
        // and does not converge to the correct marginals. Hence we use a (very
        // high) tolerance of 2 to catch the partition function's value.
        assertEqualMarginals(fg, bf, bp, 2);
    }

    private void testOnChainHelper(boolean logDomain) {
        FactorGraph fg = BruteForceInferencerTest.getLinearChainGraph(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = BpScheduleType.TREE_LIKE;
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        // Don't normalize the messages, so that the partition function is the
        // same as in the brute force approach.
        prm.normalizeMessages = false;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();

        BruteForceInferencerTest.testInfOnLinearChainGraph(fg, bp, logDomain);
                    
        assertEqualMarginals(fg, bf, bp);
    }
    
    @Test
    public void testConvergence() {
        // Test with a threshold of 0 (i.e. exact equality implies convergence)
        testConvergenceHelper(true, 0, 6);
        testConvergenceHelper(false, 0, 6);
        // Test with a threshold of 1e-3 (i.e. fewer iterations, 5, to convergence)
        testConvergenceHelper(true, 1e-3, 5);
        testConvergenceHelper(false, 1e-3, 5);
    }

    private void testConvergenceHelper(boolean logDomain, double convergenceThreshold, int expectedConvergenceIterations) {
        FactorGraph fg = BruteForceInferencerTest.getLinearChainGraph(logDomain);

        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.normalizeMessages = true;
        prm.updateOrder = BpUpdateOrder.PARALLEL;
        prm.convergenceThreshold = convergenceThreshold;        
        
        BeliefPropagation bp = null;
        
        for (int i=0; i<20; i++) {
            prm.maxIterations = i;
            bp = new BeliefPropagation(fg, prm);
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
        testCanHandleHardFactorsHelper(true, false);
        testCanHandleHardFactorsHelper(false, false);
    }
    
    @Test
    public void testCanHandleLogHardFactors() {
        testCanHandleHardFactorsHelper(true, true);
        testCanHandleHardFactorsHelper(false, true);
    }
    
    public void testCanHandleHardFactorsHelper(boolean cacheFactorBeliefs, boolean logDomain) {     
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
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.maxIterations = 10;
        prm.logDomain = logDomain;
        prm.cacheFactorBeliefs = cacheFactorBeliefs;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
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
        
        bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        bp = new BeliefPropagation(fg, prm);
        bp.run();
        System.out.println(bp.isConverged());
        assertEqualMarginals(fg, bf, bp);
    }
    
    public static void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            BeliefPropagation bp) {
        assertEqualMarginals(fg, bf, bp, 1e-13);
    }

    public static void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            BeliefPropagation bp, double tolerance) {
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
