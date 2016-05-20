package edu.jhu.pacaya.gm.train;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModelIdentity;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;

public class LogLikelihoodFactory implements MtFactory {

    private FgInferencerFactory infFactory;
    
    public LogLikelihoodFactory(FgInferencerFactory infFactory) {
        this.infFactory = infFactory;
    }
    
    @Override
    public Module<Tensor> getInstance(FgModelIdentity mid, FactorGraph fg, VarConfig goldConfig, double weight,
            int curIter, int maxIter) {
        if (weight != 1.0) {
            throw new IllegalArgumentException("Weight not supported by CLL.");
        }
        if (hasLatentVars(fg)) {
            return new MarginalLogLikelihood(mid, fg, infFactory, goldConfig);
        } else {
            return new LogLikelihood(mid, fg, infFactory, goldConfig);
        }
    }
    
    public static boolean hasLatentVars(FactorGraph fg) {
        for (Var var : fg.getVars()) {
            if (var.getType() == VarType.LATENT) {
                // Has latent variables.
                return true;
            }
        }
        return false;
    }
    
}