package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public interface VariableSplitter {

    List<DmvBoundsDelta> split(CptBounds bounds, VariableId varId);

}
