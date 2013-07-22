package edu.jhu.optimize;

import edu.jhu.util.math.Vectors;

public class FunctionOpts {

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
    
    /** Wrapper which adds the input functions. */
    public static class AddFunctions implements Function {
    
        private Function[] functions;
        
        public AddFunctions(Function... functions) {
            int numDims = functions[0].getNumDimensions();
            for (Function f : functions) {
                if (numDims != f.getNumDimensions()) {
                    throw new IllegalArgumentException("Functions have different dimension.");
                }
            }
            this.functions = functions;
        }
        
        @Override
        public double getValue(double[] point) {
            double sum = 0.0;
            for (Function f : functions) {
                sum += f.getValue(point);                
            }
            return sum;
        }
    
        @Override
        public double[] getGradient(double[] point) {
            double[] gradient = new double[getNumDimensions()];
            for (Function f : functions) {
                Vectors.add(gradient, f.getGradient(point));
            }
            return gradient;
        }
    
        @Override
        public int getNumDimensions() {
            return functions[0].getNumDimensions();
        }
    
    }

}
