package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Assert;

import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.ValueGradient;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
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
            return getOutputSize(vecFn.getInputs());
        }
        
        @Override
        public double getValue(IntDoubleVector point) {
            setInputsFromIdv(vecFn.getInputs(), point);
            MVec out = vecFn.forward();
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
            MVec outAdj = vecFn.getOutputAdj();
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
        public static int getOutputSize(List<? extends Module<? extends MVec>> mods) {
            int totInDimension = 0;
            for (Module<?> mod : mods) {
                totInDimension += mod.getOutput().size();
            }
            return totInDimension;
        }

        /**
         * Treats all the input modules as if they were concatenated into a long vector, and sets
         * their values to those in the given IntDoubleVector.
         */
        public static void setInputsFromIdv(List<? extends Module<? extends MVec>> inputs, IntDoubleVector point) {
            int idx = 0;
            for (Module<? extends MVec> input : inputs) {
                MVec x = input.getOutput();
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
        public static IntDoubleVector getInputsAsIdv(List<? extends Module<? extends MVec>> inputs) {
            int totInDimension = getOutputSize(inputs);
            IntDoubleVector grad = new IntDoubleDenseVector(totInDimension);
            int idx=0;
            for (Module<? extends MVec> input : inputs) {
                MVec xAdj = input.getOutputAdj();
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

    public static void assertGradientCorrectByFd(Module<?> vecFn, double epsilon, double delta) {
        int numParams = ModuleFn.getOutputSize(vecFn.getInputs());
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertGradientCorrectByFd(vecFn, x, epsilon, delta);
    }

    public static void assertGradientCorrectByFd(DifferentiableFunction fn, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();                
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertGradientCorrectByFd(fn, x, epsilon, delta);
    }

    public static void assertGradientCorrectByFd(Module<?> vecFn, IntDoubleVector x, double epsilon, double delta) {
        int numParams = ModuleFn.getOutputSize(vecFn.getInputs());
        if (numParams == 0) {
            throw new IllegalStateException("No input parameters!");
        }
        // Run forward once to figure out the output dimension.
        int outDim = vecFn.forward().size();
        for (int i=0; i<outDim; i++) {
            ModuleFn fn = new ModuleFn(vecFn, i);
            IntDoubleVector gradAd = fn.getGradient(x);
            IntDoubleVector gradFd = StochasticGradientApproximation.estimateGradientFd(fn, x, epsilon);
            // Assert gradients are equal.
            System.out.print("i="+i);
            assertVectorEquals(gradFd, gradAd, numParams, delta);
        }
    }
    
    public static void assertGradientCorrectByFd(DifferentiableFunction fn, IntDoubleVector x, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();
        if (numParams == 0) {
            throw new IllegalStateException("No input parameters!");
        }
        IntDoubleVector gradAd = fn.getGradient(x);
        IntDoubleVector gradFd = StochasticGradientApproximation.estimateGradientFd(fn, x, epsilon);
        // Assert gradients are equal.
        assertVectorEquals(gradFd, gradAd, numParams, delta);
    }
    
    public static void assertGradientEquals(Module<?> vecFn1, Module<?> vecFn2, IntDoubleVector x, double epsilon, double delta) {
        int numParams1 = ModuleFn.getOutputSize(vecFn1.getInputs());
        int numParams2 = ModuleFn.getOutputSize(vecFn2.getInputs());
        assertEquals(numParams1, numParams2);
        if (numParams1 == 0) {
            throw new IllegalStateException("No input parameters!");
        }
        // Run forward once to figure out the output dimension.
        int outDim1 = vecFn1.forward().size();
        int outDim2 = vecFn2.forward().size();
        assertEquals(outDim1, outDim2);
        
        // Assert outputs of forward are equal.        
        for (int i=0; i<outDim1; i++) {
            double val1 = (new ModuleFn(vecFn1, i)).getValue(x);
            double val2 = (new ModuleFn(vecFn2, i)).getValue(x);
            double relError = Math.abs(val2 - val1) / Math.max(Math.abs(val2), Math.abs(val1));
            System.out.printf("i=%d val1=%g val2=%g relError=%g\n", i, val1, val2, relError);
            assertEquals(val1, val2, delta);
        }
        // Assert gradients are equal.
        for (int i=0; i<outDim1; i++) {
            IntDoubleVector grad1 = (new ModuleFn(vecFn1, i)).getGradient(x);
            IntDoubleVector grad2 = (new ModuleFn(vecFn2, i)).getGradient(x);
            System.out.println("i="+i+":");
            assertVectorEquals(grad1, grad2, numParams1, delta);
        }
    }

    /** Asserts that the two given vectors are equal up to some tolerance threshold. */
    // TODO: Maybe move this to JUnitUtils in prim?
    private static void assertVectorEquals(IntDoubleVector grad1, IntDoubleVector grad2, int numParams, double delta) {
        for (int j=0; j<numParams; j++) {
            // Test the deriviative d/dx_j(f_i(\vec{x}))
            double dot1 = grad1.get(j);
            double dot2 = grad2.get(j);
            double relError = Math.abs(dot2 - dot1) / Math.max(Math.abs(dot2), Math.abs(dot1));
            System.out.printf("j=%d dot1=%g dot2=%g relError=%g\n", j, dot1, dot2, relError);
            System.out.flush();
            Assert.assertTrue("Expected " + dot1 + " but was " + dot2,
                    Primitives.equals(dot1, dot2, delta) || relError <= delta);
        }
    }
        
}
