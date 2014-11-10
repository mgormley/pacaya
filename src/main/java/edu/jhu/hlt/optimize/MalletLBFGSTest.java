package edu.jhu.hlt.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts.NegateFunction;
import edu.jhu.hlt.optimize.functions.L1;
import edu.jhu.hlt.optimize.functions.L2;
import edu.jhu.hlt.optimize.functions.SumSquares;
import edu.jhu.hlt.optimize.functions.XSquared;
import edu.jhu.hlt.util.math.Vectors;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
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

    public static DifferentiableFunction negate(DifferentiableFunction f) {
        return new DifferentiableFunctionOpts.NegateFunction(f);
    }
    
    protected Optimizer<DifferentiableFunction> getRegularizedOptimizer(final double l1Lambda, final double l2Lambda) {
        final Optimizer<DifferentiableFunction> opt = getOptimizer();
        
        return new Optimizer<DifferentiableFunction>() {
            
            @Override
            public boolean minimize(DifferentiableFunction function, IntDoubleVector point) {
                return optimize(function, point, false);
            }
            
            @Override
            public boolean maximize(DifferentiableFunction function, IntDoubleVector point) {
                return optimize(function, point, true);
            }
            
            public boolean optimize(DifferentiableFunction objective, IntDoubleVector point, boolean maximize) {
                L1 l1 = new L1(l1Lambda);
                L2 l2 = new L2(1.0 / l2Lambda);
                l1.setNumDimensions(objective.getNumDimensions());
                l2.setNumDimensions(objective.getNumDimensions());
                DifferentiableFunction br = new DifferentiableFunctionOpts.AddFunctions(l1, l2);

                DifferentiableFunction nbr = !maximize ? new DifferentiableFunctionOpts.NegateFunction(br) : br;
                DifferentiableFunction fn = new DifferentiableFunctionOpts.AddFunctions(objective, nbr);
                
                if (!maximize) {
                    return opt.minimize(fn, point);   
                } else {
                    return opt.maximize(fn, point);
                }
            }
        };
    }
    
    @Test
    public void testL1RegularizedOffsetNegSumSquaresMax() {
        Optimizer<DifferentiableFunction> opt = getRegularizedOptimizer(1.0, 0.0);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 0.4, -5, 11};
        double[] expected = new double[]{-0.004115611134513236, 4.486700484128761, -10.481380677780265};
        DifferentiableFunction f = negate(new SumSquares(offsets));
        JUnitUtils.assertArrayEquals(new double[]{0.0, 0.0, 0.0},
                f.getGradient(new IntDoubleDenseVector(expected)).toNativeArray(),
                1e-13);
        opt.maximize(negate(new SumSquares(offsets)), new IntDoubleDenseVector(initial));
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(expected, max, 1e-10);
    }
    
    @Test
    public void testL1RegularizedOffsetNegSumSquaresMin() {
        Optimizer<DifferentiableFunction> opt = getRegularizedOptimizer(1.0, 0.0);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 0.4, -5, 11};
        double[] expected = new double[]{-0.004115611134513236, 4.486700484128761, -10.481380677780265};
        SumSquares f = new SumSquares(offsets);
        JUnitUtils.assertArrayEquals(new double[]{0.0, 0.0, 0.0},
                f.getGradient(new IntDoubleDenseVector(expected)).toNativeArray(),
                1e-13);
        opt.minimize(f, new IntDoubleDenseVector(initial));
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(expected, max, 1e-10);
    }

    @Test
    public void testL2RegularizedOffsetNegSumSquaresMax() {
        Optimizer<DifferentiableFunction> opt = getRegularizedOptimizer(0.0, 1.0);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 0.4, -5, 11};
        opt.maximize(negate(new SumSquares(offsets)), new IntDoubleDenseVector(initial));
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(new double[]{-0.266, 3.333, -7.333}, max, 1e-3);
    }

    @Test
    public void testL2RegularizedOffsetNegSumSquaresMin() {
        Optimizer<DifferentiableFunction> opt = getRegularizedOptimizer(0.0, 1.0);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 0.4, -5, 11};
        opt.minimize(new SumSquares(offsets), new IntDoubleDenseVector(initial));
        double[] max = initial;
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(new double[]{-0.266, 3.333, -7.333}, max, 1e-3);
    }
    
}
