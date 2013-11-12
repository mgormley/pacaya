package edu.jhu.gm.inf;

import java.util.List;

import edu.jhu.gm.model.FactorGraph.FgEdge;

public interface BpSchedule {

    List<FgEdge> getOrder();

}
