package edu.jhu.gm.eval;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;

public class MseMarginalEvaluator {

    /**
     * Computes the mean squared error between the true marginals (as
     * represented by the goldConfig) and the predicted marginals (as
     * represented by an inferencer).
     * 
     * @param goldConfig The gold configuration of the variables.
     * @param inf The (already run) inferencer storing the predicted marginals.
     * @return The UNORMALIZED mean squared error.
     */
    public double evaluate(VarConfig goldConfig, FgInferencer inf) {
        double sum = 0.0;

        for (Var v : goldConfig.getVars()) {
            DenseFactor marg = inf.getMarginals(v);
            int goldState = goldConfig.getState(v);
            for (int c=0; c<marg.size(); c++) {
                double goldMarg = (c == goldState) ? 1.0 : 0.0;
                double diff = goldMarg - marg.getValue(c);
                sum += diff * diff; 
            }
        }
        
        return sum;
    }
    
}
