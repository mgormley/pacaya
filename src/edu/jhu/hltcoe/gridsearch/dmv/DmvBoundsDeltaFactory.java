package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public interface DmvBoundsDeltaFactory {

    List<DmvBoundsDelta> getDmvBounds(DmvProblemNode dmvProblemNode);

}
