package edu.jhu.hlt.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts.NegateFunction;
import edu.jhu.hlt.optimize.function.FunctionOpts;
import edu.jhu.hlt.optimize.functions.SumSquares;
import edu.jhu.hlt.optimize.functions.XSquared;
import edu.jhu.hlt.util.math.Vectors;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.JUnitUtils;

public class MalletLBFGSTest {

    protected Optimizer<DifferentiableFunction> getOptimizer() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        return new MalletLBFGS(prm);
    }

    /* ------------ Tests copied from AbstractOptimizerTest -------------- */

    @Test
    public void testNegXSquared() {
        Optimizer<DifferentiableFunction> opt = getOptimizer();
        double[] max = new double[]{ 9.0 };
        opt.maximize(new NegateFunction(new XSquared()), new IntDoubleDenseVector(max));
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    @Test
    public void testXSquared() {
        Optimizer<DifferentiableFunction> opt = getOptimizer();
        double[] max = new double[]{ 9.0 };
        opt.minimize(new XSquared(), new IntDoubleDenseVector(max));
        assertEquals(0.0, max[0], 1e-10);        
    }
    
    @Test
    public void testNegSumSquares() {
        Optimizer<DifferentiableFunction> opt = getOptimizer();
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        opt.maximize(new NegateFunction(new SumSquares(initial.length)), new IntDoubleDenseVector(initial));
        double[] max = initial;
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
    @Test
    public void testOffsetNegSumSquares() {
        Optimizer<DifferentiableFunction> opt = getOptimizer();
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 3, -5, 11};
        opt.maximize(new NegateFunction(new SumSquares(offsets)), new IntDoubleDenseVector(initial));
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(offsets, max, 1e-10);
    }
    
    
}