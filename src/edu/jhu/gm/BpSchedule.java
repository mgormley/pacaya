package edu.jhu.hltcoe.gm;

import java.util.List;

import edu.jhu.hltcoe.gm.FactorGraph.FgEdge;

public interface BpSchedule {

    List<FgEdge> getOrder();

}
