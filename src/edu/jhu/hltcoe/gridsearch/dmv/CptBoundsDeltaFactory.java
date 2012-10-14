package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public interface CptBoundsDeltaFactory {

    List<CptBoundsDelta> getDmvBounds(DmvProblemNode dmvProblemNode);

}
