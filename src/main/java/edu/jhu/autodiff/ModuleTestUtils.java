package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import edu.jhu.autodiff.erma.Beliefs;
import edu.jhu.autodiff.erma.StochasticGradientApproximation;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.dist.Gaussian;

public class ModuleTestUtils {

    private ModuleTestUtils() { }
        
    /**
     * Used to test modules by treating them as a differentiable function.
     * @author mgormley
     */
    public static class ModuleFn implements DifferentiableFunction {

        private Module<?> vecFn;
        int outIdx;
        
        public ModuleFn(Module<?> vecFn, int outIdx) {
            this.vecFn = vecFn;
            this.outIdx = outIdx;
        }
    
        @Override
        public int getNumDimensions() {
            return getInputSize(vecFn.getInputs());
        }
        
        @Override
        public double getValue(IntDoubleVector point) {
            setInputsFromIdv(vecFn.getInputs(), point);
            MVec<?> out = vecFn.forward();
            return out.getValue(outIdx);
        }
    
        @Override
        public IntDoubleVector getGradient(IntDoubleVector point) {
            setInputsFromIdv(vecFn.getInputs(), point);
            vecFn.forward();
            vecFn.zeroOutputAdj();
            for (Module<?> x : vecFn.getInputs()) {
                x.zeroOutputAdj();
            }
            MVec<?> outAdj = vecFn.getOutputAdj();
            outAdj.fill(0.0);
            outAdj.setValue(outIdx, 1.0);
            vecFn.backward();
            return getInputsAsIdv(vecFn.getInputs());
        }
    
        @Override
        public ValueGradient getValueGradient(IntDoubleVector point) {
            return new ValueGradient(getValue(point), getGradient(point));
        }
        
        /**
         * Treats all the input modules as if they were concatenated into a long vector, and
         * computes the size of that vector.
         */
        public static int getInputSize(List<? extends Module<? extends MVec<?>>> inputs) {
            int totInDimension = 0;
            for (Module<?> input : inputs) {
                totInDimension += input.getOutput().size();
            }
            return totInDimension;
        }

        /**
         * Treats all the input modules as if they were concatenated into a long vector, and sets
         * their values to those in the given IntDoubleVector.
         */
        public static void setInputsFromIdv(List<? extends Module<? extends MVec<?>>> inputs, IntDoubleVector point) {
            int idx = 0;
            for (Module<? extends MVec<?>> input : inputs) {
                MVec<?> x = input.getOutput();
                int size = x.size();
                for (int c=0; c<size; c++) {
                    x.setValue(c, point.get(idx++));
                }
            }
        }

        /**
         * Concatenates all the adjoints of a set of input modules to create a single
         * IntDoubleVector.
         */
        public static IntDoubleVector getInputsAsIdv(List<? extends Module<? extends MVec<?>>> inputs) {
            int totInDimension = getInputSize(inputs);
            IntDoubleVector grad = new IntDoubleDenseVector(totInDimension);
            int idx=0;
            for (Module<? extends MVec<?>> input : inputs) {
                MVec<?> xAdj = input.getOutputAdj();
                int size = xAdj.size();
                for (int c=0; c<size; c++) {
                    grad.set(idx++, xAdj.getValue(c));
                }
            }
            return grad;
        }
        
    }

    public static IntDoubleDenseVector getZeroOneGaussian(int numDims) {
        // Define the "model" as the explicit factor entries.
        IntDoubleDenseVector theta = new IntDoubleDenseVector(numDims);
        // Randomly initialize the model.
        for (int i=0; i< numDims; i++) {
            theta.set(i, Gaussian.nextDouble(0.0, 1.0));
        }
        return theta;
    }

    public static IntDoubleDenseVector getAbsZeroOneGaussian(int numDims) {
        // Define the "model" as the explicit factor entries.
        IntDoubleDenseVector theta = new IntDoubleDenseVector(numDims);
        // Randomly initialize the model.
        for (int i=0; i< numDims; i++) {
            theta.set(i, Math.abs(Gaussian.nextDouble(0.0, 1.0)));
        }
        return theta;
    }

    public static void assertFdAndAdEqual(Module<?> vecFn, double epsilon, double delta) {
        int numParams = ModuleFn.getInputSize(vecFn.getInputs());
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertFdAndAdEqual(vecFn, x, epsilon, delta);
    }

    public static void assertFdAndAdEqual(Module<?> vecFn, IntDoubleDenseVector x, double epsilon, double delta) {
        int numParams = ModuleFn.getInputSize(vecFn.getInputs());
        if (numParams == 0) {
            throw new IllegalStateException("No input parameters!");
        }
        // Run forward once to figure out the output dimension.
        vecFn.forward();
        int outDim = ModuleFn.getInputSize(Lists.getList(vecFn));
        for (int i=0; i<outDim; i++) {
            ModuleFn fn = new ModuleFn(vecFn, i);
            IntDoubleVector grad = fn.getGradient(x);
            for (int j=0; j<numParams; j++) {
                // Test the deriviative d/dx_j(f_i(\vec{x}))

                IntDoubleVector d = new IntDoubleDenseVector(numParams);
                d.set(j, 1);
                double dotFd = StochasticGradientApproximation.getGradDotDirApprox(fn, x, d, epsilon);
                double dotAd = grad.dot(d);
                double relError = Math.abs(dotFd - dotAd) / Math.max(Math.abs(dotFd), Math.abs(dotAd));
                System.out.printf("i=%d j=%d dotFd=%g dotAd=%g relError=%g\n", i, j, dotFd, dotAd, relError);
                assertEquals(dotFd, dotAd, delta);
            }
        }
    }
    
    public static void assertFdAndAdEqual(DifferentiableFunction fn, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();                
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertFdAndAdEqual(fn, x, epsilon, delta);
    }
    
    public static void assertFdAndAdEqual(DifferentiableFunction fn, IntDoubleVector x, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();
        if (numParams == 0) {
            throw new IllegalStateException("No input parameters!");
        }
        for (int j=0; j<numParams; j++) {
            // Test the deriviative d/dx_j(f(\vec{x}))
            IntDoubleVector d = new IntDoubleDenseVector(numParams);
            d.set(j, 1);
            double dotFd = StochasticGradientApproximation.getGradDotDirApprox(fn, x, d, epsilon);
            IntDoubleVector grad = fn.getGradient(x);
            double dotAd = grad.dot(d);
            double relError = Math.abs(dotFd - dotAd) / Math.max(Math.abs(dotFd), Math.abs(dotAd));
            System.out.printf("j=%d dotFd=%g dotAd=%g relError=%g\n", j, dotFd, dotAd, relError);
            assertEquals(dotFd, dotAd, delta);
        }
    }

}
