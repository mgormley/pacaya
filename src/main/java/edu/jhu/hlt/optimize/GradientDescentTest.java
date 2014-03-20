package edu.jhu.hlt.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.hlt.optimize.function.AbstractSlowFunction;
import edu.jhu.hlt.optimize.function.FunctionOpts;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.JUnitUtils;

public class GradientDescentTest {
    
    public static class XSquared extends AbstractSlowFunction {
        
        @Override
        public double getValue(double[] point) {
            return point[0]*point[0];
        }

        @Override
        public double[] getGradientAtPoint(double[] point) {
            return new double[] { 2*point[0] };
        }

        @Override
        public int getNumDimensions() {
            return 1;
        }
        
    }
    
    /** The function \sum_i x_i^2. */
    public static class SumSquares extends AbstractSlowFunction {
        
        private int dim;
        private double[] offsets;
        
        public SumSquares(int dim) {
            this.dim = dim;
            this.offsets = new double[dim];
        }
        
        public SumSquares(double[] offsets) {
            this.dim = offsets.length;
            this.offsets = offsets;
        }
        
        @Override
        public double getValue(double[] point) {
            point = DoubleArrays.copyOf(point);
            for (int i=0; i<point.length; i++) {
                point[i] += offsets[i];
            }
            return DoubleArrays.dotProduct(point, point);
        }

        @Override
        public double[] getGradientAtPoint(double[] point) {
            double[] gradient = new double[point.length];
            for (int i=0; i<gradient.length; i++) {
                gradient[i] = 2*(point[i] + offsets[i]);
            }
            return gradient;
        }

        @Override
        public int getNumDimensions() {
            return dim;
        }
        
    }
    
    @Test
    public void testNegXSquared() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] max = new double[]{ 9.0 };
        opt.maximize(new FunctionOpts.NegateFunction(new XSquared()), max);
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    @Test
    public void testXSquared() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] max = new double[]{ 9.0 };
        opt.minimize(new XSquared(), max);
        assertEquals(0.0, max[0], 1e-10);        
    }
    
    @Test
    public void testNegSumSquares() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(initial.length)), initial);
        double[] max = initial;
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
    @Test
    public void testOffsetNegSumSquares() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 3, -5, 11};
        opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(offsets)), initial);
        double[] max = initial;
        DoubleArrays.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(offsets, max, 1e-10);
    }
}
