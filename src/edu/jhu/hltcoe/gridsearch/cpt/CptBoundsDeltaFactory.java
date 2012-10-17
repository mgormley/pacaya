package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;

public interface CptBoundsDeltaFactory {

    List<CptBoundsDelta> getDmvBounds(DmvProblemNode dmvProblemNode);

}
