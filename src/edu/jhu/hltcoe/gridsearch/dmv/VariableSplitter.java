package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public interface VariableSplitter {

    List<DmvBoundsDelta> split(DmvBounds bounds, VariableId varId);

}
