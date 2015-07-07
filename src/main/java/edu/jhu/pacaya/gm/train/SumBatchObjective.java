package edu.jhu.pacaya.gm.train;

import edu.jhu.pacaya.gm.model.FgModel;

/**
 * An objective function which is computed for a set of examples. 
 * The objective is always of the form:
 * 
 * f(\theta) = \sum_{i=1}^N g_i(\theta)
 * 
 * where N is the number of examples, f is this objective function, g_i is the
 * objective function for the i^th example.
 * 
 * The gradient and value of the objective function are obtained from the
 * {@link ExampleObjective#getValue} and {@link ExampleObjective#addGradient}
 * methods.
 * 
 * @author mgormley
 */
public class SumBatchObjective extends AvgBatchObjective {
    
    public SumBatchObjective(ExampleObjective exObj, FgModel model, int numThreads) {
        super(exObj, model, numThreads);
    }

    protected double getDivisorForAveraging(int[] batch, Accumulator ac) {
        return 1.0;
    }
    
}
