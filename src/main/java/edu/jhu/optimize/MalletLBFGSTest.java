package edu.jhu.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.optimize.FunctionOpts.NegateFunction;
import edu.jhu.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.optimize.SGDTest.SumSquares;
import edu.jhu.optimize.SGDTest.XSquared;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.math.Vectors;

public class MalletLBFGSTest {
    
    @Test
    public void testNegXSquared() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        MalletLBFGS opt = new MalletLBFGS(prm);
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new XSquared()), new double[]{ 9.0 });
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    // TODO: support minimization.
    //    @Test
    //    public void testXSquared() {
    //        MalletLBFGSPrm prm = new MalletLBFGSPrm();
    //        MalletLBFGS opt = new MalletLBFGS(prm);
    //        double[] max = opt.minimize(new XSquared(), new double[]{ 9.0 });
    //        assertEquals(0.0, max[0], 1e-10);        
    //    }
    
    @Test
    public void testNegSumSquares() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        MalletLBFGS opt = new MalletLBFGS(prm);
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(initial.length)), initial);
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
    @Test
    public void testOffsetNegSumSquares() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        MalletLBFGS opt = new MalletLBFGS(prm);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 3, -5, 11};
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(offsets)), initial);
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(offsets, max, 1e-10);
    }
    
}
