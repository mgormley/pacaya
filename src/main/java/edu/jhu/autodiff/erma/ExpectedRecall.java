package edu.jhu.autodiff.erma;

import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;

public class ExpectedRecall {
    
    private ErmaBp bp;
    private VarConfig vc;
    // Output
    private double expectedRecall;
    // Adjoints of input.
    private VarTensor[] varBeliefsAdjs;
    private VarTensor[] facBeliefsAdjs;
    
    public ExpectedRecall(ErmaBp bp, VarConfig vc) {
        this.bp = bp;
        this.vc = vc;
    }
    
    public void forward() {
        expectedRecall = 0;
        for (Var var : vc.getVars()) {
            VarTensor marg = bp.getMarginals(var);
            expectedRecall += marg.getValue(vc.getState(var));
        }
    }
    
    public void backward(double expectedRecallAdj) {
        FactorGraph fg = bp.getFactorGraph();
        varBeliefsAdjs = new VarTensor[fg.getNumVars()];
        
        // Initialize the variable belief adjoints to 0.
        for (int v=0; v<varBeliefsAdjs.length; v++) {
            varBeliefsAdjs[v] = new VarTensor(new VarSet(fg.getVar(v)), 0);
        }
        // Fill in the non-zero adjoints with the adjoint of the expected recall.
        for (Var var : vc.getVars()) {
            varBeliefsAdjs[var.getId()].setValue(vc.getState(var), expectedRecallAdj);
        }
        
        facBeliefsAdjs = new VarTensor[fg.getNumFactors()];
        for (int a=0; a<facBeliefsAdjs.length; a++) {
            facBeliefsAdjs[a] = new VarTensor(fg.getFactor(a).getVars(), 0);
        }
    }
    
    public double getExpectedRecall() {
        return expectedRecall;
    }

    public VarTensor[] getVarBeliefsAdjs() {
        return varBeliefsAdjs;
    }

    public VarTensor[] getFacBeliefsAdjs() {
        return facBeliefsAdjs;
    }

}
