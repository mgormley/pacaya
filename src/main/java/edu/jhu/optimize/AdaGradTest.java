package edu.jhu.optimize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import edu.jhu.optimize.GradientDescentTest.SumSquares;
import edu.jhu.optimize.GradientDescentTest.XSquared;
import edu.jhu.optimize.AdaGrad.AdaGradPrm;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.math.Vectors;

public class AdaGradTest {

    @Test
    public void testActualBatchFunction() {
        fail("Not yet implemented");
    }
    
    @Test
    public void testNegXSquared() {
        AdaGrad opt = getNewSgd(0.1, 100);
        double[] max = new double[]{ 9.0 };
        opt.maximize(negate(bf(new XSquared())), max);
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    @Test
    public void testXSquared() {
        AdaGrad opt = getNewSgd(0.1, 100);
        double[] max = new double[]{ 9.0 };
        opt.minimize(bf(new XSquared()), max);
        assertEquals(0.0, max[0], 1e-10);        
    }
    
    @Test
    public void testNegSumSquares() {
        AdaGrad opt = getNewSgd(0.1, 100);
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        opt.maximize(negate(bf(new SumSquares(initial.length))), initial);
        double[] max = initial;
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
    @Test
    public void testOffsetNegSumSquares() {
        AdaGrad opt = getNewSgd(0.1, 100);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 3, -5, 11};
        opt.maximize(negate(bf(new SumSquares(offsets))), initial);
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(offsets, max, 1e-10);
    }
    
    public static AdaGrad getNewSgd(double eta, int numPasses) {
        AdaGradPrm prm = new AdaGradPrm();
        prm.eta = eta * 100;
        prm.sgdPrm.numPasses = numPasses;        
        prm.sgdPrm.batchSize = 1;
        return new AdaGrad(prm);
    }
    
    public static BatchFunction bf(Function f) {
        return new FunctionAsBatchFunction(f, 10);
    }
    
    public static BatchFunction negate(BatchFunction bf) {
        return new BatchFunctionOpts.NegateFunction(bf);
    }
}
