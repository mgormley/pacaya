package edu.jhu.optimize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.util.JUnitUtils;
import edu.jhu.util.math.Vectors;

public class SGDTest {
    
    /** Wrapper which negates the input function. */
    public static class NegateFunction implements Function {

        private Function function;
        
        public NegateFunction(Function function) {
            this.function = function;
        }
        
        @Override
        public double getValue(double[] point) {
            return - function.getValue(point);
        }

        @Override
        public double[] getGradient(double[] point) {
            double[] gradient = function.getGradient(point);
            Vectors.scale(gradient, -1.0);
            return gradient;
        }

        @Override
        public int getNumDimensions() {
            return function.getNumDimensions();
        }

    }
    
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
        
        public SumSquares(int dim) {
            this.dim = dim;
        }
        
        @Override
        public double getValue(double[] point) {
            return Vectors.dotProduct(point, point);
        }

        @Override
        public double[] getGradient(double[] point) {
            double[] gradient = new double[point.length];
            for (int i=0; i<gradient.length; i++) {
                gradient[i] = 2*point[i];
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
        SGD opt = new SGD(0.1, 100);
        double[] max = opt.maximize(new NegateFunction(new XSquared()), new double[]{ 9.0 });
        assertEquals(0.0, max[0], 1e-10);      
    }
    
    @Test
    public void testXSquared() {
        SGD opt = new SGD(0.1, 100);
        double[] max = opt.minimize(new XSquared(), new double[]{ 9.0 });
        assertEquals(0.0, max[0], 1e-10);        
    }
    
    @Test
    public void testNegSumSquares() {
        SGD opt = new SGD(0.1, 100);
        double[] initial = new double[3];
        initial[0] = 9;
        initial[1] = 2;
        initial[2] = -7;
        double[] max = opt.maximize(new NegateFunction(new SumSquares(initial.length)), initial);
        JUnitUtils.assertArrayEquals(new double[] {0.0, 0.0, 0.0} , max, 1e-10);
    }
    
}
