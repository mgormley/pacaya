package edu.jhu.pacaya.gm.inf;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.pacaya.gm.data.bayesnet.BayesNetReaderTest;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphTest;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.JUnitUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;

public class BruteForceInferencerTest {

    @Test
    public void testOnChainProb() {
        // Test in the probability domain.
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        FactorGraph fg = getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        testInfOnLinearChainGraph(fg, bf);
    }
    
    @Test
    public void testOnChainLogProb() {
        // Test in the log-probability domain.
        Algebra s = LogSemiring.LOG_SEMIRING;
        FactorGraph fg = getLinearChainGraph();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        testInfOnLinearChainGraph(fg, bf);
    }

    /**
     * 
        VarTensor [
             A     B     C  |  value
            YY   Yes   Yes  |  0.800000
            YY   Yes    No  |  0.200000
            YY    No   Yes  |  0.500000
            YY    No    No  |  0.500000
            NN   Yes   Yes  |  0.600000
            NN   Yes    No  |  0.400000
            NN    No   Yes  |  0.100000
            NN    No    No  |  0.900000
        ]
        VarTensor [
             A     B  |  value
            YY   Yes  |  0.600000
            YY    No  |  0.400000
            NN   Yes  |  0.150000
            NN    No  |  0.850000
        ]
             * Current  joint: 
        VarTensor [
             A     B     C  |  value
            YY   Yes   Yes  |  0.480000
            YY   Yes    No  |  0.120000
            YY    No   Yes  |  0.200000
            YY    No    No  |  0.200000
            NN   Yes   Yes  |  0.0900000
            NN   Yes    No  |  0.0600000
            NN    No   Yes  |  0.0850000
            NN    No    No  |  0.765000
        ]
     * @throws IOException
     */
    @Test
    public void testOnSimpleProb() throws IOException {
        // Test in the probability domain.
        Algebra s = RealAlgebra.REAL_ALGEBRA;
        FactorGraph fg = readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
        bf.run();
        System.out.println("joint:\n" + bf.getJointFactor());
        testInfOnSimpleGraph(fg, bf);
    }

    @Test
    public void testOnSimpleLogProb() throws IOException {
        // Test in the log-probability domain.
        Algebra s = LogSemiring.LOG_SEMIRING;
        FactorGraph fg = readSimpleFg();
        BruteForceInferencer bf = new BruteForceInferencer(fg, s);
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
        goldMarg = new double[] { 0.013146806000337095, 0.06607112759143771, 0.1774818810045508, 0.7433001854036744 };
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
        goldMarg = new double[] { 0.3, 0.2, 0.075, 0.425 };
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
