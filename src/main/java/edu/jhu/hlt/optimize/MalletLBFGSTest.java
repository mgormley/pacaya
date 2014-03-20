package edu.jhu.hlt.optimize;

import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.function.DifferentiableFunction;

public class MalletLBFGSTest extends AbstractOptimizerTest {

    @Override
    protected Optimizer<DifferentiableFunction> getOptimizer() {
        MalletLBFGSPrm prm = new MalletLBFGSPrm();
        return new MalletLBFGS(prm);
    }

}