package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.ModuleTensor;
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

/**
 * Module for computing exp(\theta \cdot f(x,y)) to populate each of the exponential family factors.
 * 
 * @author mgormley
 */
public class ExpFamFactorsModule implements Module<Factors> {

    private FgModel model;
    private FgModel modelAdj;
    private FactorGraph fg;
    private Factors y;
    private Factors yAdj;
    private Algebra s;
    
    public ExpFamFactorsModule(FactorGraph fg, FgModel model, Algebra s) {
        this.fg = fg;
        this.model = model;
        this.s = s;
    }
    
    @Override
    public Factors forward() {
        fg.updateFromModel(model);
        y = new Factors(s);
        y.f = new VarTensor[fg.getNumFactors()];
        for (int a = 0; a < y.f.length; a++) {
            Factor factor = fg.getFactor(a);
            if (!(factor instanceof GlobalFactor)) {
                y.f[a] = BruteForceInferencer.safeNewVarTensor(s, factor);
                assert !y.f[a].containsBadValues();
            }
        }
        return y;
    }

    @Override
    public void backward() {
        modelAdj = model.getSparseZeroedCopy();
        for (int a = 0; a < y.f.length; a++) {
            Factor factor = fg.getFactor(a);
            if (!(factor instanceof GlobalFactor)) {
                VarTensor factorMarginal = new VarTensor(yAdj.f[a]);
                factorMarginal.prod(y.f[a]);
                // addExpectedFeatureCounts() currently only supports the real semiring
                factorMarginal = factorMarginal.copyAndConvertAlgebra(Algebras.REAL_ALGEBRA);
                factor.addExpectedFeatureCounts(modelAdj, factorMarginal, Algebras.REAL_ALGEBRA.one());
            }
        }
    }

    @Override
    public Factors getOutput() {
        return y;
    }

    @Override
    public Factors getOutputAdj() {
        if (yAdj == null) {
            yAdj = new Factors(s);
            yAdj.f = new VarTensor[y.f.length];
            for (int a = 0; a < yAdj.f.length; a++) {
                if (y.f[a] != null) {
                    yAdj.f[a] = new VarTensor(s, y.f[a].getVars(), s.zero());
                }
            }
        }
        return yAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) {
            for (int a = 0; a < yAdj.f.length; a++) {
                if (yAdj.f[a] != null) { yAdj.f[a].fill(s.zero()); }
            }
        }
    }

    @Override
    public List<Module<? extends ModuleTensor>> getInputs() {
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
