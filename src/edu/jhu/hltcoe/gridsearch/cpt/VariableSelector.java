package edu.jhu.hltcoe.gridsearch.cpt;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDmvSolution;

public interface VariableSelector {

    VariableId select(DmvProblemNode node, DmvRelaxation relax, RelaxedDmvSolution relaxSol);

}
