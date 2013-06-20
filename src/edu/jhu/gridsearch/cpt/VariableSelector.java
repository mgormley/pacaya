package edu.jhu.gridsearch.cpt;

import edu.jhu.gridsearch.dmv.DmvProblemNode;
import edu.jhu.gridsearch.dmv.DmvRelaxation;
import edu.jhu.gridsearch.dmv.DmvRelaxedSolution;

public interface VariableSelector {

    VariableId select(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol);

}
