package edu.jhu.pacaya.gm.train;

import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.autodiff.erma.Factors;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.util.collections.Lists;

public class LogLikelihood extends AbstractModule<Scalar> implements Module<Scalar> {

    private FgInferencer inf;
    private Module<Factors> factors;
    private VarConfig goldConfig;

    public LogLikelihood(Module<Factors> factors, FgInferencer inf, VarConfig goldConfig) {
        super(factors.getAlgebra());
        this.factors = factors;
        this.inf = inf;
        this.goldConfig = goldConfig;
    }

    @Override
    public Scalar forward() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void backward() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<? extends Module<? extends MVec>> getInputs() {
        return Lists.getList(factors);
    }

}
