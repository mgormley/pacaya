package edu.jhu.gm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.gm.data.BayesNetReaderTest;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

public class BruteForceInferencerTest {

    @Test
    public void testOnChainProb() {
        // Test in the probability domain.
        boolean logDomain = false;
        FactorGraph fg = getLinearChainGraph(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnLinearChainGraph(fg, bf, logDomain);
    }
    
    @Test
    public void testOnChainLogProb() {
        // Test in the log-probability domain.
        boolean logDomain = true;
        FactorGraph fg = getLinearChainGraph(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnLinearChainGraph(fg, bf, logDomain);
    }

    @Test
    public void testOnSimpleProb() throws IOException {
        // Test in the probability domain.
        boolean logDomain = false;
        FactorGraph fg = readSimpleFg(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnSimpleGraph(fg, bf, logDomain);
    }

    @Test
    public void testOnSimpleLogProb() throws IOException {
        // Test in the log-probability domain.
        boolean logDomain = true;
        FactorGraph fg = readSimpleFg(logDomain);
        BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
        testInfOnSimpleGraph(fg, bf, logDomain);
    }

    public static void testInfOnLinearChainGraph(FactorGraph fg,
            FgInferencer bp, boolean logDomain) {        
        bp.run();

        Factor marg;
        double[] goldMarg;
        
        marg = bp.getMarginals(fg.getVar(0));
        goldMarg = new double[] { 0.079, 0.920 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), 1e-2);
        
        marg = bp.getMarginals(fg.getFactor(3));
        goldMarg = new double[] { 0.013146806000337095, 0.1774818810045508, 0.06607112759143771, 0.7433001854036744 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), logDomain ? 1e-2 : 1e-4);
        
        double goldPartition = 0.5932;
        if (logDomain) { goldPartition = Utilities.log(goldPartition); }
        assertEquals(goldPartition, bp.getPartition(), 1e-2);
    }
    
    public static void testInfOnSimpleGraph(FactorGraph fg, FgInferencer bp, boolean logDomain) {
        bp.run();

        // System.out.println(bp.getJointFactor());

        Factor marg;
        double[] goldMarg;
        
        assertEquals("A", fg.getVar(0).getName());
        marg = bp.getMarginals(fg.getVar(0));
        goldMarg = new double[] { 0.5, 0.5 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), 1e-2);

        assertEquals("B", fg.getVar(1).getName());
        marg = bp.getMarginals(fg.getVar(1));
        goldMarg = new double[] { 0.375, 0.625 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), 1e-3);

        assertEquals("C", fg.getVar(2).getName());
        marg = bp.getMarginals(fg.getVar(2));
        goldMarg = new double[] { 0.4275, 0.5725 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), 1e-4);
        
        assertEquals(new VarSet(fg.getVar(0), fg.getVar(1)), fg.getFactor(0).getVars());
        marg = bp.getMarginals(fg.getFactor(0));
        goldMarg = new double[] { 0.3, 0.075, 0.2, 0.425 };
        if (logDomain) { goldMarg = Vectors.getLog(goldMarg); }
        JUnitUtils.assertArrayEquals(goldMarg,
                marg.getValues(), 1e-3);
        
        double goldPartition = 2.00;
        if (logDomain) { goldPartition = Utilities.log(goldPartition); }
        assertEquals(goldPartition, bp.getPartition(), 1e-2);
    }
    
    public static FactorGraph getLinearChainGraph(boolean logDomain) {
        FactorGraph fg = FactorGraphTest.getLinearChainGraph(logDomain);
        return fg;
    }

    
    public static FactorGraph readSimpleFg(boolean logDomain) throws IOException {
        FactorGraph fg = BayesNetReaderTest.readSimpleFg(logDomain);
        return fg;
    }
}
