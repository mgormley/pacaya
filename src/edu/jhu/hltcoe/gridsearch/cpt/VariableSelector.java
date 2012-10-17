package edu.jhu.hltcoe.gridsearch.cpt;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;

public interface VariableSelector {

    VariableId select(DmvProblemNode node);

}
