package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.RealAlgebra;

/**
 * Mean squared error (MSE) loss function.
 * 
 * This computes the MSE for a single example without the 1/N scaling factor.
 * 
 * @author mgormley
 */
public class MeanSquaredError extends AbstractTensorModule implements Module<Tensor> {
    
    /** Factory for MSE loss without a decoder. */
    public static class ExpectedRecallFactory implements DlFactory {
        @Override
        public Module<Tensor> getDl(FactorGraph fg, VarConfig goldConfig, Module<Beliefs> inf, Algebra s) {
            return new MeanSquaredError(inf, goldConfig, s);
        }
    }
    
    private Module<Beliefs> inf;
    private VarConfig vc;
    private Algebra s;
    // Output
    private double mse;
    
    public MeanSquaredError(Module<Beliefs> inf, VarConfig vc, Algebra s) {
        this.inf = inf;
        this.vc = vc;
        this.s = s;
    }

    /**
     * Forward pass: y = \sum_{x_i} (b(x_i) - b*(x_i))^2 , where b*(x_i) are the marginals given by
     * taking the gold variable assignment as a point mass distribution. Note the sum is over all
     * variable assignments to the predicted variables.
     */
    public Tensor forward() {
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        double mse = s.zero();

        for (Var v : vc.getVars()) {
            if (v.getType() == VarType.PREDICTED) {
                VarTensor marg = varBeliefs[v.getId()];
                int goldState = vc.getState(v);
                for (int c=0; c<marg.size(); c++) {
                    double goldMarg = (c == goldState) ? s.one() : s.zero();
                    double predMarg = marg.getValue(c);
                    double diff = s.minus(Math.max(goldMarg, predMarg), Math.min(goldMarg, predMarg));
                    mse = s.plus(mse, s.times(diff, diff));
                }
            }
        }
        y = Tensor.getScalarTensor(s.toReal(mse));
        return y;        
    }
    
    /** 
     * Backward pass: 
     * 
     * Expanding the square, we get y = \sum_{x_i} b(x_i)^2 - 2b(x_i)b*(x_i) + b*(x_i)^2
     * 
     * dG/db(x_i) = dG/dy dy/db(x_i) = dG/dy 2(b(x_i) - b*(x_i)), \forall x_i. 
     */
    public void backward() {
        double mseAdj = yAdj.getValue(0);
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        VarTensor[] varBeliefsAdjs = inf.getOutputAdj().varBeliefs;
        
        // Fill in the non-zero adjoints with the adjoint of the expected recall.
        for (Var v : vc.getVars()) {
            if (v.getType() == VarType.PREDICTED) {
                VarTensor marg = varBeliefs[v.getId()];
                int goldState = vc.getState(v);
                for (int c=0; c<marg.size(); c++) {
                    double goldMarg = (c == goldState) ? s.one() : s.zero();
                    double predMarg = marg.getValue(c);
                    double diff = s.minus(predMarg, goldMarg);
                    // dG/db(x_i) = dG/dy 2(b(x_i) - b*(x_i))
                    double adj_b = s.times(mseAdj, s.plus(diff, diff));
                    // Increment adjoint of the belief.
                    varBeliefsAdjs[v.getId()].addValue(c, adj_b);
                }
            }
        }
    }
    
    public double getMse() {
        return mse;
    }
    
    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList(inf);
    }

}
