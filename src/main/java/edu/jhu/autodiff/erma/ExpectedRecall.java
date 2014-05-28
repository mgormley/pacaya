package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.AbstractTensorModule;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.collections.Lists;

public class ExpectedRecall extends AbstractTensorModule implements Module<Tensor> {
    
    private Module<Beliefs> inf;
    private VarConfig vc;
    // Output
    private double expectedRecall;
    
    public ExpectedRecall(Module<Beliefs> inf, VarConfig vc) {
        this.inf = inf;
        this.vc = vc;
    }
    
    public Tensor forward() {
        VarTensor[] varBeliefs = inf.getOutput().varBeliefs;
        expectedRecall = 0;
        for (Var var : vc.getVars()) {
            VarTensor marg = varBeliefs[var.getId()];
            expectedRecall += marg.getValue(vc.getState(var));
        }
        y = Tensor.getScalarTensor(expectedRecall);
        return y;
    }

    public void backward() {
        double expectedRecallAdj = yAdj.getValue(0);               
        VarTensor[] varBeliefsAdjs = inf.getOutputAdj().varBeliefs;
        
        // Fill in the non-zero adjoints with the adjoint of the expected recall.
        for (Var var : vc.getVars()) {
            varBeliefsAdjs[var.getId()].addValue(vc.getState(var), expectedRecallAdj);
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
