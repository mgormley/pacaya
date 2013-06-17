package edu.jhu.hltcoe.gm;

import edu.jhu.hltcoe.gm.FactorGraph.Factor;
import edu.jhu.hltcoe.gm.FactorGraph.Var;
import edu.jhu.hltcoe.gm.FactorGraph.VarSet;

/**
 * Loopy belief propagation inference algorithm.
 * 
 * @author mgormley
 *
 */
public class BeliefPropagation implements FgInferencer {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Factor getMarginals(Var var) {
        return getMarginals(new VarSet(var));
    }

    @Override
    public Factor getMarginals(VarSet varSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginals(Factor factor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForVarId(int varId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getLogPartition(FgExample ex, FgModel model) {
        // TODO Auto-generated method stub
        return 0;
    }
    


}
