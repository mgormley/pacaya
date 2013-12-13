package edu.jhu.globalopt.cpt;

import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvRelaxedSolution;

public interface VariableSelector {

    VariableId select(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol);

}
