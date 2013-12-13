package edu.jhu.globalopt.cpt;

import java.util.List;


public interface VariableSplitter {

    List<CptBoundsDeltaList> split(CptBounds bounds, VariableId varId);

}
