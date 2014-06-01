package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.Module;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.util.collections.Lists;

/**
 * Module for computing exp(\theta \cdot f(x,y)) to populate each of the exponential family factors.
 * 
 * @author mgormley
 */
public class ExpFamFactorModule implements Module<VarTensor[]> {

    private FgModel model;
    private FgModel modelAdj;
    private FactorGraph fg;
    private boolean logDomain;
    private VarTensor[] y;
    private VarTensor[] yAdj;
    
    public ExpFamFactorModule(FactorGraph fg, FgModel model, boolean logDomain) {
        this.fg = fg;
        this.model = model;
        this.logDomain = logDomain;
    }
    
    @Override
    public VarTensor[] forward() {
        fg.updateFromModel(model, logDomain);
        y = new VarTensor[fg.getNumFactors()];
        for (int a = 0; a < y.length; a++) {
            Factor factor = fg.getFactor(a);
            if (!(factor instanceof GlobalFactor)) {
                y[a] = BruteForceInferencer.safeGetVarTensor(factor);
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
                factor.addExpectedFeatureCounts(modelAdj, factorMarginal, 1.0);
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
                    yAdj[a] = new VarTensor(y[a].getVars(), 0.0); // TODO: semiring
                }
            }
        }
        return yAdj;
    }

    @Override
    public void zeroOutputAdj() {
        if (yAdj != null) {
            for (int a = 0; a < yAdj.length; a++) {
                if (yAdj[a] != null) { yAdj[a].fill(0.0); }
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

}
