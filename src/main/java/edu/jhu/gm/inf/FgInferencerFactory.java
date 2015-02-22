package edu.jhu.gm.inf;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.util.semiring.Algebra;

public interface FgInferencerFactory {

    FgInferencer getInferencer(FactorGraph fg);

    Algebra getAlgebra();

}