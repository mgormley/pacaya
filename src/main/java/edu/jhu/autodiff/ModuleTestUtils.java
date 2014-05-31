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
     * Vector function mapping R^n ==> R^m.
     * Used to test modules by treating them as a differentiable function.
     * @author mgormley
     */
    public interface VecFn {
        int getNumDimensions();
        Tensor getOutputAdj();
        Tensor forward(IntDoubleVector point);
        IntDoubleVector forwardAndBackward(IntDoubleVector point, Tensor outAdj);        
    }
    
    /**
     * Vector function that has a list of tensor modules as input.
     * @author mgormley
     */
    public static class TensorVecFn implements VecFn {
    
        List<Module<Tensor>> inputs; 
        Module<Tensor> output;
        int totInDimension;
        
        public TensorVecFn(List<Module<Tensor>> inputs, Module<Tensor> output) {
            // TODO: find the inputs (i.e. leaves) by DFS.
            this.inputs = inputs;
            this.output = output;
            computeTotInDimension();
        }
        
        public TensorVecFn(Module<Tensor> input, Module<Tensor> output) {
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

        public Tensor getOutputAdj() {
            return output.getOutputAdj();
        }
        
    }

    /**
     * Vector function that has a list of beliefs modules as input.
     * @author mgormley
     */
    public static class BeliefsVecFn implements VecFn {
    
        List<Module<Beliefs>> inputs; 
        Module<Tensor> output;
        int totInDimension;
        
        public BeliefsVecFn(List<Module<Beliefs>> inputs, Module<Tensor> output) {
            // TODO: find the inputs (i.e. leaves) by DFS.
            this.inputs = inputs;
            this.output = output;
            computeTotInDimension();
        }
        
        public BeliefsVecFn(Module<Beliefs> input, Module<Tensor> output) {
            this(Lists.getList(input), output);
        }
    
        private void computeTotInDimension() {
            totInDimension = 0;
            for (Module<Beliefs> input : inputs) {
                totInDimension += input.getOutput().size();
            }
        }
    
        public int getNumDimensions() {
            return totInDimension;
        }
        
        public Tensor forward(IntDoubleVector point) {
            // Populate the inputs with the point.
            int idx = 0;
            for (Module<Beliefs> input : inputs) {
                Beliefs b = input.getOutput();
                for (int i=0; i<2; i++) {
                    VarTensor[] beliefs = (i==0) ? b.varBeliefs : b.facBeliefs;
                    for (int j=0; j<beliefs.length; j++) {
                        VarTensor x = beliefs[j];
                        int size = x.size();
                        for (int c=0; c<size; c++) {
                            x.setValue(c, point.get(idx++));
                        }
                    }
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
            int idx = 0;
            for (Module<Beliefs> input : inputs) {
                Beliefs b = input.getOutputAdj();
                for (int i=0; i<2; i++) {
                    VarTensor[] beliefs = (i==0) ? b.varBeliefs : b.facBeliefs;
                    for (int j=0; j<beliefs.length; j++) {
                        VarTensor x = beliefs[j];
                        int size = x.size();
                        for (int c=0; c<size; c++) {
                            grad.set(idx++, x.getValue(c));
                        }
                    }
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

        public Tensor getOutputAdj() {
            return output.getOutputAdj();
        }
        
    }
    
    /**
     * Used to test modules by treating them as a differentiable function.
     * @author mgormley
     */
    public static class ModuleFn implements DifferentiableFunction {

        private VecFn vecFn;
        int outIdx;
        
        public ModuleFn(VecFn vecFn, int outIdx) {
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
            Tensor outAdj = vecFn.getOutputAdj().copy();
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

    public static void assertFdAndAdEqual(VecFn vecFn, double epsilon, double delta) {
        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertFdAndAdEqual(vecFn, x, epsilon, delta);
    }

    public static void assertFdAndAdEqual(VecFn vecFn, IntDoubleDenseVector x, double epsilon, double delta) {
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
    
    public static void assertFdAndAdEqual(DifferentiableFunction fn, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();                
        IntDoubleDenseVector x = getZeroOneGaussian(numParams);
        assertFdAndAdEqual(fn, x, epsilon, delta);
    }
    
    public static void assertFdAndAdEqual(DifferentiableFunction fn, IntDoubleVector x, double epsilon, double delta) {
        int numParams = fn.getNumDimensions();
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

    public static Tensor getVector(double... values) {
        Tensor t1 = new Tensor(values.length);
        for (int c=0; c<values.length; c++) {
            t1.setValue(c, values[c]);
        }
        return t1;
    }

    public static Tensor get2DTensor(int s1, int s2) {
        Tensor t1 = new Tensor(s1, s2);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            for (int j=0; j<s2; j++) {
                t1.set(val++, i,j);
            }
        }
        return t1;
    }

    public static Tensor get3DTensor(int s1, int s2, int s3) {
        Tensor t1 = new Tensor(s1, s2, s3);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            for (int j=0; j<s2; j++) {
                for (int k=0; k<s3; k++) {
                    t1.set(val++, i,j,k);            
                }
            }
        }
        return t1;
    }


}
