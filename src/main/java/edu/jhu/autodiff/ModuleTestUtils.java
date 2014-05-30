package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import java.util.List;

import edu.jhu.autodiff.erma.StochasticGradientApproximation;
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
    public static class ModuleVecFn {
    
        List<Module<Tensor>> inputs; 
        Module<Tensor> output;
        int totInDimension;
        
        public ModuleVecFn(List<Module<Tensor>> inputs, Module<Tensor> output) {
            this.inputs = inputs;
            this.output = output;
            computeTotInDimension();
        }
        
        public ModuleVecFn(Module<Tensor> input, Module<Tensor> output) {
            this(Lists.getList(input), output);
        }
    
        private void computeTotInDimension() {
            totInDimension = 0;
            for (Module<Tensor> input : inputs) {
                totInDimension += input.getOutput().size();
            }
        }
    
        public int getNumDimensions() {
            return totInDimension;
        }
        
        public Tensor forward(IntDoubleVector point) {
            // Populate the inputs with the point.
            int idx = 0;
            for (Module<Tensor> input : inputs) {
                Tensor x = input.getOutput();
                int size = x.size();
                for (int c=0; c<size; c++) {
                    x.setValue(c, point.get(idx++));
                }
            }
            // Run the forward pass.
            return output.forward();
        }
        
        public IntDoubleVector forwardAndBackward(IntDoubleVector point, Tensor outputAdj) {
            zeroOutputAdjs(output);
            // Run the forward pass.
            forward(point);
            // Set the adjoint.
            output.getOutputAdj().set(outputAdj);
            // Run the backward pass.
            output.backward();
            
            // Populate the output vector with the adjoints.
            IntDoubleVector grad = new IntDoubleDenseVector(totInDimension);
            int idx=0;
            for (Module<Tensor> input : inputs) {
                Tensor xAdj = input.getOutputAdj();
                int size = xAdj.size();
                for (int c=0; c<size; c++) {
                    grad.set(idx++, xAdj.getValue(c));
                }
            }
            return grad;
        }

        private void zeroOutputAdjs(Module<? extends Object> output) {
            output.zeroOutputAdj();
            for (Object input : output.getInputs()) {
                zeroOutputAdjs((Module)input);
            }
        }

        public Module<Tensor> getOutput() {
            return output;
        }
        
    }
    
    /**
     * Used to test modules by treating them as a differentiable function.
     * @author mgormley
     */
    public static class ModuleFn implements DifferentiableFunction {

        private ModuleVecFn vecFn;
        int outIdx;
        
        public ModuleFn(ModuleVecFn vecFn, int outIdx) {
            this.vecFn = vecFn;
            this.outIdx = outIdx;
        }
    
        @Override
        public int getNumDimensions() {
            return vecFn.getNumDimensions();
        }
        
        @Override
        public double getValue(IntDoubleVector point) {
            Tensor out = vecFn.forward(point);
            //System.out.println("ModuleFn:\n" + point);
            //System.out.println("ModuleFn:\n" + out);
            return out.getValue(outIdx);
        }
    
        @Override
        public IntDoubleVector getGradient(IntDoubleVector point) {
            Tensor outAdj = vecFn.getOutput().getOutputAdj().copy();
            outAdj.fill(0);
            outAdj.setValue(outIdx, 1);
            
            return vecFn.forwardAndBackward(point, outAdj);
        }
    
        @Override
        public ValueGradient getValueGradient(IntDoubleVector point) {
            return new ValueGradient(getValue(point), getGradient(point));
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

    public static Tensor getVector(double... values) {
        Tensor t1 = new Tensor(values.length);
        for (int c=0; c<values.length; c++) {
            t1.setValue(c, values[c]);
        }
        return t1;
    }

    public static void assertFdAndAdEqual(ModuleVecFn vecFn, double epsilon, double delta) {
        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertFdAndAdEqual(vecFn, x, epsilon, delta);
    }

    public static void assertFdAndAdEqual(ModuleVecFn vecFn, IntDoubleDenseVector x, double epsilon, double delta) {
        int numParams = vecFn.getNumDimensions();                
        // Run forward once to figure out the output dimension.
        int outDim = vecFn.forward(x).size();
        for (int i=0; i<outDim; i++) {
            for (int j=0; j<numParams; j++) {
                // Test the deriviative d/dx_j(f_i(\vec{x}))
                ModuleFn fn = new ModuleFn(vecFn, i);
                IntDoubleVector d = new IntDoubleDenseVector(numParams);
                d.set(j, 1);
                double dotFd = StochasticGradientApproximation.getGradDotDirApprox(fn, x, d, epsilon);
                IntDoubleVector grad = fn.getGradient(x);
                double dotAd = grad.dot(d);
                double relError = Math.abs(dotFd - dotAd) / Math.max(Math.abs(dotFd), Math.abs(dotAd));
                System.out.printf("i=%d j=%d dotFd=%g dotAd=%g relError=%g\n", i, j, dotFd, dotAd, relError);
                assertEquals(dotFd, dotAd, delta);
            }
        }
    }


}
