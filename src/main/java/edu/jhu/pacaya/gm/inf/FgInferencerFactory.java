package edu.jhu.pacaya.gm.inf;

import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.util.semiring.Algebra;

public interface FgInferencerFactory {

    FgInferencer getInferencer(FactorGraph fg);

    Algebra getAlgebra();

}