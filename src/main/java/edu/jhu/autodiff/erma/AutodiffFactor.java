package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.Module;
import edu.jhu.util.semiring.Algebra;

public interface AutodiffFactor {

    Module<?> getFactorModule(Module<MVecFgModel> modIn, Algebra s);

}
