package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;

public class RandomMpSchedule implements MpSchedule {

    private ArrayList<FgEdge> order;
    
    public RandomMpSchedule(FactorGraph fg) {
        order = new ArrayList<FgEdge>(fg.getEdges());
    }
    @Override
    public List<FgEdge> getOrder() {
        Collections.shuffle(order);
        return order;
    }

}
