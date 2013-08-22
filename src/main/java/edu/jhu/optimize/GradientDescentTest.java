package edu.jhu.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Vectors;

public class GradientDescentTest {
    
    public static class XSquared implements Function {
        
        @Override
        public double getValue(double[] point) {
            return point[0]*point[0];
        }

        @Override
        public double[] getGradient(double[] point) {
            return new double[] { 2*point[0] };
        }

        @Override
        public int getNumDimensions() {
            return 1;
        }
        
    }
    
    /** The function \sum_i x_i^2. */
    public static class SumSquares implements Function {
        
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
            point = Utilities.copyOf(point);
            for (int i=0; i<point.length; i++) {
                point[i] += offsets[i];
            }
            return Vectors.dotProduct(point, point);
        }

        @Override
        public double[] getGradient(double[] point) {
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
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new XSquared()), new double[]{ 9.0 });
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    @Test
    public void testXSquared() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] max = opt.minimize(new XSquared(), new double[]{ 9.0 });
        assertEquals(0.0, max[0], 1e-10);        
    }
    
    @Test
    public void testNegSumSquares() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(initial.length)), initial);
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
    @Test
    public void testOffsetNegSumSquares() {
        GradientDescent opt = new GradientDescent(0.1, 100);
        double[] initial = new double[] { 9, 2, -7};
        double[] offsets = new double[] { 3, -5, 11};
        double[] max = opt.maximize(new FunctionOpts.NegateFunction(new SumSquares(offsets)), initial);
        Vectors.scale(offsets, -1.0);
        JUnitUtils.assertArrayEquals(offsets, max, 1e-10);
    }
}
