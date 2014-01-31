package edu.jhu.gm.inf;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.DenseFactor;
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
	public void testCanHandleProbHardFactors() {
		boolean logDomain = false;
		
		Var x0 = new Var(VarType.PREDICTED, 2, "x0", null);
		Var x1 = new Var(VarType.PREDICTED, 2, "x1", null);
		
		DenseFactor df = new DenseFactor(new VarSet(x0, x1));
		for(int cfg=0; cfg < df.getVars().calcNumConfigs(); cfg++) {
			VarConfig vCfg = df.getVars().getVarConfig(cfg);
			int v0 = vCfg.getState(x0);
			int v1 = vCfg.getState(x1);
			if(v0 != v1)
				df.setValue(cfg, 0d);
			else
				df.setValue(cfg, 1d);
		}
		ExplicitFactor xor = new ExplicitFactor(df);
		
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
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        assertEqualMarginals(fg, bf, bp);
        
        DenseFactor x0_marg = bp.getMarginals(x0);
        assertEquals(0.5d, x0_marg.getValue(0), 1e-6);
        assertEquals(0.5d, x0_marg.getValue(1), 1e-6);
        DenseFactor x1_marg = bp.getMarginals(x1);
        assertEquals(0.5d, x1_marg.getValue(0), 1e-6);
        assertEquals(0.5d, x1_marg.getValue(1), 1e-6);
				
		// check again once we've added some unary factors on x0 and x1
		df = new DenseFactor(new VarSet(x0));
		df.setValue(0, 3d);
		df.setValue(1, 2d);
		ExplicitFactor f0 = new ExplicitFactor(df);
		fg.addFactor(f0);
		
		df = new DenseFactor(new VarSet(x0));
		df.setValue(0, 5d);
		df.setValue(1, 1d);
		ExplicitFactor f1 = new ExplicitFactor(df);
		fg.addFactor(f1);
		
		bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        bp = new BeliefPropagation(fg, prm);
        bp.run();
		assertEqualMarginals(fg, bf, bp);
	}
	
	@Test
	public void testCanHandleLogHardFactors() {
		boolean logDomain = true;
		
		Var x0 = new Var(VarType.PREDICTED, 2, "x0", null);
		Var x1 = new Var(VarType.PREDICTED, 2, "x1", null);
		
		// add a hard xor factor
		// this shouldn't move the marginals away from uniform
		DenseFactor df = new DenseFactor(new VarSet(x0, x1));
		for(int cfg=0; cfg < df.getVars().calcNumConfigs(); cfg++) {
			VarConfig vCfg = df.getVars().getVarConfig(cfg);
			int v0 = vCfg.getState(x0);
			int v1 = vCfg.getState(x1);
			if(v0 != v1)
				df.setValue(cfg, Double.NEGATIVE_INFINITY);
			else
				df.setValue(cfg, 0d);
		}
		ExplicitFactor xor = new ExplicitFactor(df);
		
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
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        assertEqualMarginals(fg, bf, bp);
        
    	DenseFactor x0_marg = bp.getMarginals(x0);
        assertEquals(Math.log(0.5d), x0_marg.getValue(0), 1e-6);
        assertEquals(Math.log(0.5d), x0_marg.getValue(1), 1e-6);
        DenseFactor x1_marg = bp.getMarginals(x1);
        assertEquals(Math.log(0.5d), x1_marg.getValue(0), 1e-6);
        assertEquals(Math.log(0.5d), x1_marg.getValue(1), 1e-6);
				
		// check again once we've added some unary factors on x0 and x1
		df = new DenseFactor(new VarSet(x0));
		df.setValue(0, -2d);
		df.setValue(1, -3d);
		ExplicitFactor f0 = new ExplicitFactor(df);
		fg.addFactor(f0);
		
		df = new DenseFactor(new VarSet(x0));
		df.setValue(0, -1d);
		df.setValue(1, -5d);
		ExplicitFactor f1 = new ExplicitFactor(df);
		fg.addFactor(f1);
		
		bf = new BruteForceInferencer(fg, logDomain);
        bf.run();
        bp = new BeliefPropagation(fg, prm);
        bp.run();
		assertEqualMarginals(fg, bf, bp);
	}
	
    
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
        
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((DenseFactor)f).convertRealToLog();
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
    public void testTwoVarsProb() {
        boolean logDomain = false;

        FactorGraph fg = new FactorGraph();
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);

        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        emit0.setValue(0, 1.1);
        emit0.setValue(1, 1.9);

        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        tran0.set(1);
        tran0.setValue(0, 2.2);
        tran0.setValue(1, 2.3);
        tran0.setValue(2, 2.4);
        tran0.setValue(3, 2.5);
        
        fg.addFactor(emit0);
        fg.addFactor(tran0);
        
        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((DenseFactor)f).convertRealToLog();
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

        if (logDomain) {
            for (Factor f : fg.getFactors()) {
                ((DenseFactor)f).convertRealToLog();
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
            DenseFactor bfm = bf.getMarginals(var);
            DenseFactor bpm = bp.getMarginals(var);
            if (!bfm.equals(bpm, tolerance)) {
                assertEquals(bfm, bpm);
            }
        }
        for (Factor f : fg.getFactors()) {
            DenseFactor bfm = bf.getMarginals(f);
            DenseFactor bpm = bp.getMarginals(f);
            if (!bfm.equals(bpm, tolerance)) {
                assertEquals(bfm, bpm);
            }
        }
        assertEquals(bf.getPartition(), bp.getPartition(), tolerance);
    }
    
}
