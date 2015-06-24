package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.Lists;

/**
 * Computes the L2 distance between the "true beliefs" and the predicted beliefs.
 * 
 * @author mgormley
 */
public class L2Distance extends AbstractModule<Tensor> implements Module<Tensor> {
    
    /** Factory for L2 distance loss without a decoder. */
    public static class MeanSquaredErrorFactory implements DlFactory {
        @Override
        public Module<Tensor> getDl(VarConfig goldConfig, FactorsModule effm, Module<Beliefs> inf, int curIter, int maxIter) {
            return new L2Distance(inf, goldConfig);
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(L2Distance.class);

    private Module<Beliefs> inf;
    private VarConfig vc;
    
    public L2Distance(Module<Beliefs> inf, VarConfig vc) {
        super(inf.getAlgebra());
        this.inf = inf;
        this.vc = vc;
    }

    /**
     * Forward pass: y = \sum_{x_i} (b(x_i) - b*(x_i))^2 , where b*(x_i) are the marginals given by
     * taking the gold variable assignment as a point mass distribution. Note the sum is over all
     * variable assignments to the predicted variables.
     */
    public Tensor forward() {
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        double l2dist = s.zero();

        for (Var v : vc.getVars()) {
            if (v.getType() == VarType.PREDICTED) {
                VarTensor marg = varBeliefs[v.getId()];
                int goldState = vc.getState(v);
                for (int c=0; c<marg.size(); c++) {
                    double goldMarg = (c == goldState) ? s.one() : s.zero();
                    double predMarg = marg.getValue(c);
                    double diff = s.minus(Math.max(goldMarg, predMarg), Math.min(goldMarg, predMarg));
                    l2dist = s.plus(l2dist, s.times(diff, diff));
                }
            }
        }
        y = Tensor.getScalarTensor(s, l2dist);
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
        double l2distAdj = yAdj.getValue(0);
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
                    // dG/db(x_i) = dG/dy 2(b(x_i) - b*(x_i))
                    double diff = s.minus(predMarg, goldMarg);
                    double adj_b = s.times(l2distAdj, s.plus(diff, diff));
                    // Increment adjoint of the belief.
                    varBeliefsAdjs[v.getId()].addValue(c, adj_b);
                }
            }
        }
    }
    
    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList(inf);
    }

}
