package edu.jhu.gm;

import java.util.List;

import edu.jhu.gm.FactorGraph.FgEdge;

public interface BpSchedule {

    List<FgEdge> getOrder();

}
