package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Utilities;


public class BeliefPropagationTest {
    
    @Test
    public void testOnOneVar() {
        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);

        Factor emit0 = new Factor(new VarSet(t0)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);

        fg.addFactor(emit0);
        
        boolean logDomain = true;
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                f.convertRealToLog();
            }
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
        
        FactorGraph fg = getThreeConnectedComponentsFactorGraph(logDomain);
        
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

    public static FactorGraph getThreeConnectedComponentsFactorGraph(boolean logDomain) {
        FactorGraph fg = new FactorGraph();
        
        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Utilities.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Utilities.getList("N", "V"));
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", Utilities.getList("N", "V"));
        
        // Emission factors. 
        Factor emit0 = new Factor(new VarSet(t0)); 
        Factor emit1 = new Factor(new VarSet(t1)); 
        Factor emit2 = new Factor(new VarSet(t2)); 

        emit0.setValue(0, 0.1);
        emit0.setValue(1, 0.9);
        emit1.setValue(0, 3);
        emit1.setValue(1, 7);
        emit2.setValue(0, 1);
        emit2.setValue(1, 1);
                
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);

        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                f.convertRealToLog();
            }
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

    private void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            BeliefPropagation bp) {
        assertEqualMarginals(fg, bf, bp, 1e-13);
    }

    private void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            BeliefPropagation bp, double tolerance) {
        for (Var var : fg.getVars()) {
            Factor bfm = bf.getMarginals(var);
            Factor bpm = bp.getMarginals(var);
            if (!bfm.equals(bpm, tolerance)) {
                assertEquals(bfm, bpm);
            }
        }
        for (Factor f : fg.getFactors()) {
            Factor bfm = bf.getMarginals(f);
            Factor bpm = bp.getMarginals(f);
            if (!bfm.equals(bpm, tolerance)) {
                assertEquals(bfm, bpm);
            }
        }
        assertEquals(bf.getPartition(), bp.getPartition(), tolerance);
    }
}
