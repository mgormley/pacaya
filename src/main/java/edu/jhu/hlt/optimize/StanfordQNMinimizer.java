package edu.jhu.hlt.optimize;

import org.apache.log4j.Logger;

import edu.jhu.hlt.optimize.function.DifferentiableFunction;
import edu.jhu.hlt.optimize.function.DifferentiableFunctionOpts;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;
import edu.stanford.nlp.optimization.DiffFunction;
import edu.stanford.nlp.optimization.Minimizer;
import edu.stanford.nlp.optimization.QNMinimizer;

public class StanfordQNMinimizer implements Optimizer<DifferentiableFunction> {

    private static final Logger log = Logger.getLogger(StanfordQNMinimizer.class);
    private int maxLbfgsIterations = 1000;

    public StanfordQNMinimizer(int maxLbfgsIterations) {
        this.maxLbfgsIterations  = maxLbfgsIterations;
    }
    
    @Override
    public boolean maximize(DifferentiableFunction fn, IntDoubleVector x) {
        return minimize(DifferentiableFunctionOpts.negate(fn), x);
    }

    @Override
    public boolean minimize(final DifferentiableFunction fn, IntDoubleVector xVec) {
        // The initial point.
        double[] point = new double[fn.getNumDimensions()];
        double[] x = point;
        double[] xArr = x;
        setArrayFromVector(xArr, xVec);

        // Construct the function.
        DiffFunction function = new DiffFunction(){

            @Override
            public double valueAt(double[] x) {
                return fn.getValue(new IntDoubleDenseVector(x));
            }

            @Override
            public int domainDimension() {
                return fn.getNumDimensions();
            }

            @Override
            public double[] derivativeAt(double[] x) {
                 IntDoubleVector g = fn.getGradient(new IntDoubleDenseVector(x));
                 if (g instanceof IntDoubleDenseVector) {
                     IntDoubleDenseVector gVec = (IntDoubleDenseVector)g;
                     double[] gArr = gVec.getInternalElements();
                     if (gArr.length != fn.getNumDimensions()) {
                         log.warn("Internal representation of gradient is incorrectly sized.");
                         gArr = DoubleArrays.copyOf(gArr, fn.getNumDimensions());
                     }
                     return gArr;
                 } else {
                     throw new RuntimeException("Gradients should be returned as DenseIntDoubleVectors for efficiency");
                 }                 
            }
        };
        
        // Minimize.
        // the number of gradients to keep around.
        int m = 15;
        double functionTolerance = 1e-4;
        Minimizer<DiffFunction> min = new QNMinimizer(m);
        min.minimize(function, functionTolerance, xArr, maxLbfgsIterations);
        
        // Return the minima.
        setVectorFromArray(xVec, xArr);
        return true;

        // Sidenote: A ColumnDataClassifier would create a Linear classifier as below in makeClassifier().
        // 
        //        LinearClassifierFactory<String,String> lcf;
        //        lcf  = new LinearClassifierFactory<String,String>(globalFlags.tolerance, globalFlags.useSum, globalFlags.prior, globalFlags.sigma, globalFlags.epsilon, globalFlags.QNsize);
        //        lc = lcf.trainClassifier(train);
    }

    private void setArrayFromVector(double[] point, IntDoubleVector x) {
        for (int i=0; i<point.length; i++) {
            point[i] = x.get(i); 
        }
    }

    private void setVectorFromArray(IntDoubleVector x, double[] point) {
        for (int i=0; i<point.length; i++) {
            x.set(i, point[i]); 
        }
    }

}
