package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.util.semiring.Algebra;

public interface BeliefsModuleFactory {
    Module<Beliefs> getBeliefsModule(Module<Factors> fm, FactorGraph fg);
    Algebra getAlgebra();
}