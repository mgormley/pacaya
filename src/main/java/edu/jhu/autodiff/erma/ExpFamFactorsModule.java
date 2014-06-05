package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.Module;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.RealAlgebra;

/**
 * Module for computing exp(\theta \cdot f(x,y)) to populate each of the exponential family factors.
 * 
 * @author mgormley
 */
public class ExpFamFactorsModule implements Module<VarTensor[]> {

    private FgModel model;
    private FgModel modelAdj;
    private FactorGraph fg;
    private VarTensor[] y;
    private VarTensor[] yAdj;
    private Algebra s;
    
    public ExpFamFactorsModule(FactorGraph fg, FgModel model, Algebra s) {
        if (!(s.equals(Algebras.LOG_SEMIRING) || s.equals(Algebras.REAL_ALGEBRA))) {
            throw new IllegalArgumentException("Unsupported algebra: " + s);
        }
        this.fg = fg;
        this.model = model;
        this.s = s;
    }
    
    @Override
    public VarTensor[] forward() {
        fg.updateFromModel(model);
        y = new VarTensor[fg.getNumFactors()];
        for (int a = 0; a < y.length; a++) {
            Factor factor = fg.getFactor(a);
            if (!(factor instanceof GlobalFactor)) {
                y[a] = BruteForceInferencer.safeNewVarTensor(s, factor);
                assert !y[a].containsBadValues();
            }
        }
        return y;
    }

    @Override
    public void backward() {
        modelAdj = model.getSparseZeroedCopy();
        for (int a = 0; a < y.length; a++) {
            Factor factor = fg.getFactor(a);
            if (!(factor instanceof GlobalFactor)) {
                VarTensor factorMarginal = new VarTensor(yAdj[a]);
                factorMarginal.prod(y[a]);
                assert s.equals(Algebras.REAL_ALGEBRA) : "addExpectedFeatureCounts() currently only supports the real semiring";
                factor.addExpectedFeatureCounts(modelAdj, factorMarginal, s.one());
            }
        }
    }

    @Override
    public VarTensor[] getOutput() {
        return y;
    }

    @Override
    public VarTensor[] getOutputAdj() {
        if (yAdj == null) {
            yAdj = new VarTensor[y.length];
            for (int a = 0; a < yAdj.length; a++) {
                if (y[a] != null) {
                    yAdj[a] = new VarTensor(s, y[a].getVars(), s.zero());
                }
            }
        }
        return yAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) {
            for (int a = 0; a < yAdj.length; a++) {
                if (yAdj[a] != null) { yAdj[a].fill(s.zero()); }
            }
        }
    }

    @Override
    public List<? extends Object> getInputs() {
        return Lists.getList();
    }

    public FgModel getModelAdj() {
        return modelAdj;
    }

    @Override
    public Algebra getAlgebra() {
        return s;
    }

}
