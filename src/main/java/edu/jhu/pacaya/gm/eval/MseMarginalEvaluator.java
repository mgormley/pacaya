package edu.jhu.pacaya.gm.eval;

import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

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
        Algebra s = new RealAlgebra();
        double sum = s.zero();

        for (Var v : goldConfig.getVars()) {
            if (v.getType() == VarType.PREDICTED) {
                VarTensor marg = inf.getMarginals(v);
                int goldState = goldConfig.getState(v);
                for (int c=0; c<marg.size(); c++) {
                    double goldMarg = (c == goldState) ? s.one() : s.zero();
                    double predMarg = marg.getValue(c);
                    double diff = s.minus(Math.max(goldMarg, predMarg), Math.min(goldMarg, predMarg));
                    sum = s.plus(sum, s.times(diff, diff));
                }
            }
        }
        return s.toReal(sum);
    }
    
}
