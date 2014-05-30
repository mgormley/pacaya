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

/**
 * Loss function computing the expected recall from variable beliefs and an assignment to the
 * variables.
 * 
 * @author mgormley
 */
public class ExpectedRecall extends AbstractTensorModule implements Module<Tensor> {
    
    /** Factory for expected recall loss without a decoder. */
    public static class ExpectedRecallFactory implements DlFactory {
        @Override
        public Module<Tensor> getDl(FactorGraph fg, VarConfig goldConfig, Module<Beliefs> inf, Algebra s) {
            return new ExpectedRecall(inf, goldConfig);
        }        
    }
    
    private Module<Beliefs> inf;
    private VarConfig vc;
    // Output
    private double expectedRecall;    
    
    public ExpectedRecall(Module<Beliefs> inf, VarConfig vc) {
        this.inf = inf;
        this.vc = vc;
    }
    
    /** Forward pass: y = \sum_{x_i \in x*} b(x_i), where x* is the gold variable assignment. */
    public Tensor forward() {
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        expectedRecall = 0;
        for (Var var : vc.getVars()) {
            if (var.getType() == VarType.PREDICTED) {
                VarTensor marg = varBeliefs[var.getId()];
                expectedRecall += marg.getValue(vc.getState(var));
            }
        }
        y = Tensor.getScalarTensor(expectedRecall);
        return y;
    }
    
    /** Backward pass: dG/db(x_i) = dG/dy dy/db(x_i) = dG/dy, \forall x_i \in x*. */
    public void backward() {
        double expectedRecallAdj = yAdj.getValue(0);               
        VarTensor[] varBeliefsAdjs = inf.getOutputAdj().varBeliefs;
        
        // Fill in the non-zero adjoints with the adjoint of the expected recall.
        for (Var var : vc.getVars()) {
            if (var.getType() == VarType.PREDICTED) {
                varBeliefsAdjs[var.getId()].addValue(vc.getState(var), expectedRecallAdj);
            }
        }
    }
    
    public double getExpectedRecall() {
        return expectedRecall;
    }
    
    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList(inf);
    }

}
