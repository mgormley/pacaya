package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public interface DmvBoundsFactory {

    List<DmvBounds> getDmvBounds(DmvProblemNode dmvProblemNode);

}
