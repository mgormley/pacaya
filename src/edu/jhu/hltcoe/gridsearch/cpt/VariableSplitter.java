package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.List;


public interface VariableSplitter {

    List<CptBoundsDelta> split(CptBounds bounds, VariableId varId);

}
