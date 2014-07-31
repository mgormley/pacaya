package edu.jhu.autodiff.erma;

import java.util.List;

import org.apache.log4j.Logger;

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

    private static final Logger log = Logger.getLogger(ExpFamFactorsModule.class);
    
    private Module<MVecFgModel> modIn;
    private FactorGraph fg;
    private Factors y;
    private Factors yAdj;
    private Algebra s;
    
    public ExpFamFactorsModule(Module<MVecFgModel> modIn, FactorGraph fg, Algebra s) {
        this.modIn = modIn;
        this.fg = fg;
        this.s = s;
    }
    
    @Override
    public Factors forward() {
        FgModel model = modIn.getOutput().getModel();
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
        // Get a sparse zeroed copy of the model.
        FgModel modelAdj = modIn.getOutputAdj().getModel();
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
            yAdj = y.copyAndFill(s.zero());
        }
        return yAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) {
            yAdj.fill(s.zero());
        }
    }

    @Override
    public List<Module<MVecFgModel>> getInputs() {
        return Lists.getList(modIn);
    }

    @Override
    public Algebra getAlgebra() {
        return s;
    }

}
