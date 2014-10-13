package edu.jhu.gm.inf;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.gm.data.bayesnet.BayesNetReaderTest;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.JUnitUtils;

public class BruteForceInferencerTest {

    @Test
    public void testOnChainProb() {
        // Test in the probability domain.
        boolean logDomain = false;
        FactorGraph fg = getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnLinearChainGraph(fg, bf);
    }
    
    @Test
    public void testOnChainLogProb() {
        // Test in the log-probability domain.
        boolean logDomain = true;
        FactorGraph fg = getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnLinearChainGraph(fg, bf);
    }

    @Test
    public void testOnSimpleProb() throws IOException {
        // Test in the probability domain.
        boolean logDomain = false;
        FactorGraph fg = readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnSimpleGraph(fg, bf);
    }

    // TODO: This test passes when run by itself, but fails when run together with other tests!
    @Test
    public void testOnSimpleLogProb() throws IOException {
        // Test in the log-probability domain.
        boolean logDomain = true;
        FactorGraph fg = readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnSimpleGraph(fg, bf);
    }

    public static void testInfOnLinearChainGraph(FactorGraph fg,
            FgInferencer bp) {        
        bp.run();

        VarTensor marg;
        double[] goldMarg;
        
        marg = bp.getMarginals(fg.getVar(0));
        goldMarg = new double[] { 0.079, 0.920 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-2);
        marg = bp.getLogMarginals(fg.getVar(0));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-2);
        
        marg = bp.getMarginals(fg.getFactor(3));
        goldMarg = new double[] { 0.013146806000337095, 0.1774818810045508, 0.06607112759143771, 0.7433001854036744 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-4);
        marg = bp.getLogMarginals(fg.getFactor(3));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-4);
        
        double goldPartition = 0.5932;
        assertEquals(goldPartition, bp.getPartition(), 1e-2);
        assertEquals(FastMath.log(goldPartition), bp.getLogPartition(), 1e-2);
    }
    
    public static void testInfOnSimpleGraph(FactorGraph fg, FgInferencer bp) {
        bp.run();

        // System.out.println(bp.getJointFactor());

        VarTensor marg;
        double[] goldMarg;
        
        assertEquals("A", fg.getVar(0).getName());
        marg = bp.getMarginals(fg.getVar(0));
        goldMarg = new double[] { 0.5, 0.5 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-2);
        //
        marg = bp.getLogMarginals(fg.getVar(0));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-2);

        assertEquals("B", fg.getVar(1).getName());
        marg = bp.getMarginals(fg.getVar(1));
        goldMarg = new double[] { 0.375, 0.625 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);
        //
        marg = bp.getLogMarginals(fg.getVar(1));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);

        assertEquals("C", fg.getVar(2).getName());
        marg = bp.getMarginals(fg.getVar(2));
        goldMarg = new double[] { 0.4275, 0.5725 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);
        //
        marg = bp.getLogMarginals(fg.getVar(2));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);
        
        assertEquals(new VarSet(fg.getVar(0), fg.getVar(1)), fg.getFactor(1).getVars());
        marg = bp.getMarginals(fg.getFactor(1));
        goldMarg = new double[] { 0.3, 0.075, 0.2, 0.425 };
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);
        marg = bp.getLogMarginals(fg.getFactor(1));
        goldMarg = DoubleArrays.getLog(goldMarg);
        JUnitUtils.assertArrayEquals(goldMarg, marg.getValues(), 1e-3);
        
        double goldPartition = 2.00;
        assertEquals(goldPartition, bp.getPartition(), 1e-2);
        assertEquals(FastMath.log(goldPartition), bp.getLogPartition(), 1e-2);
    }
    
    public static FactorGraph getLinearChainGraph() {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph();
        return fg;
    }

    
    public static FactorGraph readSimpleFg() throws IOException {
        FactorGraph fg = BayesNetReaderTest.readSimpleFg();
        return fg;
    }
}
