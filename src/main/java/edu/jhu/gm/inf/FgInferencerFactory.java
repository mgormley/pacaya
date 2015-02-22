package edu.jhu.gm.inf;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.util.semiring.Algebra;

public interface FgInferencerFactory {

    FgInferencer getInferencer(FactorGraph fg, FgModel model);

    Algebra getAlgebra();

}