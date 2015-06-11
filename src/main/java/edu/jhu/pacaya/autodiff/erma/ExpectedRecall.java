package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.ErmaObjective.DlFactory;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.Lists;

/**
 * Loss function computing the negative expected recall from variable beliefs and an assignment to the
 * variables.
 * 
 * @author mgormley
 */
public class ExpectedRecall extends AbstractModule<Tensor> implements Module<Tensor> {
    
    /** Factory for expected recall loss without a decoder. */
    public static class ExpectedRecallFactory implements DlFactory {
        @Override
        public Module<Tensor> getDl(VarConfig goldConfig, FactorsModule effm, Module<Beliefs> inf, int curIter, int maxIter) {
            return new ExpectedRecall(inf, goldConfig);
        }        
    }
    
    private Module<Beliefs> inf;
    private VarConfig vc;
    
    public ExpectedRecall(Module<Beliefs> inf, VarConfig vc) {
        super(inf.getAlgebra());
        this.inf = inf;
        this.vc = vc;
    }
    
    /** Forward pass: y = - \sum_{x_i \in x*} b(x_i), where x* is the gold variable assignment. */
    public Tensor forward() {
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        double expectedRecall = s.zero();
        for (Var var : vc.getVars()) {
            if (var.getType() == VarType.PREDICTED) {
                VarTensor marg = varBeliefs[var.getId()];
                expectedRecall = s.minus(expectedRecall, marg.getValue(vc.getState(var)));
            }
        }
        y = Tensor.getScalarTensor(s, expectedRecall);
        return y;
    }
    
    /** Backward pass: dG/db(x_i) = dG/dy dy/db(x_i) = - dG/dy, \forall x_i \in x*. */
    public void backward() {
        double expectedRecallAdj = yAdj.getValue(0);               
        VarTensor[] varBeliefsAdjs = inf.getOutputAdj().varBeliefs;
        
        // Fill in the non-zero adjoints with the adjoint of the expected recall.
        for (Var var : vc.getVars()) {
            if (var.getType() == VarType.PREDICTED) {
                varBeliefsAdjs[var.getId()].subtractValue(vc.getState(var), expectedRecallAdj);
            }
        }
    }
    
    @Override
    public List<Module<Beliefs>> getInputs() {
        return Lists.getList(inf);
    }

}
