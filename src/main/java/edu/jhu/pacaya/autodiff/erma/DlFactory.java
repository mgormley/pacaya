package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.model.VarConfig;

public interface DlFactory {
    /** Get a module which decodes then evaluates the loss. */
    Module<Tensor> getDl(VarConfig goldConfig, FactorsModule fm, Module<Beliefs> inf, int curIter, int maxIter);
}