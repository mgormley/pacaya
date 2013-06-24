package edu.jhu.gm;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.data.BayesNetReaderTest;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;


public class BeliefPropagationTest {
    
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

    private void testOnSimpleHelper(boolean logDomain) throws IOException {
        FactorGraph fg = BayesNetReaderTest.readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();


        BeliefPropagationPrm prm = new BeliefPropagationPrm(fg);
        prm.maxIterations = 100;
        prm.logDomain = logDomain;
        BeliefPropagation bp = new BeliefPropagation(prm);
        bp.run();

        BruteForceInferencerTest.testInfOnLinearChainGraph(fg, bp, logDomain);

        assertEqualMarginals(fg, bf, bp);
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

    private void testOnChainHelper(boolean logDomain) {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        bf.run();

        BeliefPropagationPrm prm = new BeliefPropagationPrm(fg);
        prm.maxIterations = 1;
        prm.logDomain = logDomain;
        prm.schedule = new BfsBpSchedule(fg);
        prm.updateOrder = BpUpdateOrder.SEQUENTIAL; 
        BeliefPropagation bp = new BeliefPropagation(prm);
        bp.run();

        BruteForceInferencerTest.testInfOnLinearChainGraph(fg, bp, logDomain);
                    
        assertEqualMarginals(fg, bf, bp);
    }

    private void assertEqualMarginals(FactorGraph fg, BruteForceInferencer bf,
            BeliefPropagation bp) {
        for (Var var : fg.getVars()) {
            Factor bfm = bf.getMarginals(var);
            Factor bpm = bp.getMarginals(var);
            if (!bfm.equals(bpm, 1e-13)) {
                assertEquals(bfm, bpm);
            }
        }        
        for (Factor f : fg.getFactors()) {
            Factor bfm = bf.getMarginals(f);
            Factor bpm = bp.getMarginals(f);
            if (!bfm.equals(bpm, 1e-13)) {
                assertEquals(bfm, bpm);
            }
        }
        assertEquals(bf.getPartition(), bp.getPartition(), 1e-13);
    }
}
